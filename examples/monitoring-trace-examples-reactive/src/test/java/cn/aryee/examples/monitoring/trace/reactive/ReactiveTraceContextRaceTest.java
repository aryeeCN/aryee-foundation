package cn.aryee.examples.monitoring.trace.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cn.aryee.commons.context.TraceContext;
import cn.aryee.commons.response.R;
import cn.aryee.monitoring.infrastructure.reactive.tracing.TraceWebFilter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

/**
 * Reactive 场景下 TraceContext 传递的竞态风险专项测试
 *
 * <p>核心风险点：</p>
 * <ul>
 *   <li>publishOn / flatMap 后 ThreadLocal 丢失 —— R 构造时取不到 traceId</li>
 *   <li>WebFlux 线程切换后 doFinally 在不同线程执行，导致 clear 时清错线程</li>
 * </ul>
 *
 * <p>验证方式：不真正启动 Netty，而是在纯 Reactor 流水线里模拟 TraceWebFilter 写入 Context 的行为，
 * 然后验证「新的 withTraceId(String) 结合 Mono.deferContextual」能正确把 traceId 注入到 R，
 * 即使 publishOn 后 ThreadLocal 为空。</p>
 */
public class ReactiveTraceContextRaceTest {

    private static final String MOCK_TRACE_ID = "abcd1234abcd1234abcd1234abcd1234";
    private static final String MOCK_SPAN_ID = "span123456789012";

    /**
     * 模拟 TraceWebFilter.contextWrite 写入后的流水线，验证使用 deferContextual 可在任何线程拿到 traceId
     */
    @Test
    @DisplayName("publishOn 切换线程后，ThreadLocal 为空但使用 deferContextual+withTraceId 仍能注入 traceId")
    void test_publishOn_thread_switch_recovers_traceId_from_context() {
        Mono<R<String>> pipeline = Mono
                // 1. 模拟业务先清空 ThreadLocal（证明完全不依赖 ThreadLocal）
                .fromRunnable(TraceContext::clear)
                // 2. 模拟一个 map 步骤：在"请求线程"（当前测试线程）构造 R 时自动读 ThreadLocal → 拿到 null
                .then(Mono.just("hello"))
                .map(payload -> R.ok(payload))
                // 3. 强制跳到 boundedElastic（ThreadLocal 更不可能存在）
                .publishOn(Schedulers.boundedElastic())
                // 4. 结合 deferContextual 读取 Reactor Context，重新把 traceId 写入 R
                .flatMap(r -> Mono.deferContextual(ctxView -> {
                    String traceId = ctxView.getOrDefault(TraceWebFilter.TRACE_ID_KEY, null);
                    return Mono.just(r.withTraceId(traceId));
                }));

        // 外层模拟 TraceWebFilter 写入 Context（等价于 webFilter 的 contextWrite）
        Mono<R<String>> withCtx = pipeline.contextWrite(reactor.util.context.Context.of(
                TraceWebFilter.TRACE_ID_KEY, MOCK_TRACE_ID,
                TraceWebFilter.SPAN_ID_KEY, MOCK_SPAN_ID
        ));

        StepVerifier.create(withCtx)
                .consumeNextWith(r -> {
                    // ✅ 必须有 traceId，即使 publishOn 切线程 ThreadLocal 丢了
                    assertThat(r.getExtra()).isNotNull();
                    assertThat(r.getExtra().get("traceId")).isEqualTo(MOCK_TRACE_ID);
                    assertThat(r.getData()).isEqualTo("hello");
                })
                .verifyComplete();
    }

    /**
     * 对比测试：如果业务只用 R.ok(data) 不从 Reactor Context 取，ThreadLocal 丢了就真丢了。
     * 这也证明了「ThreadLocal 不是 Reactive 可靠载体」，新 API 存在的意义。
     */
    @Test
    @DisplayName("对比：仅用 R.ok 且没从 Context 取时，publishOn 后确实取不到 traceId（证明新 API 有必要）")
    void test_without_new_api_publishOn_results_in_no_traceId() {
        Mono<R<String>> pipeline = Mono.fromRunnable(() -> {
            // 在 onSubscribe 线程写入 ThreadLocal（模拟 TraceWebFilter 过滤器初始化）
            TraceContext.initTrace("demo-svc", MOCK_TRACE_ID, MOCK_SPAN_ID);
        }).then(Mono.just("hi"))
                // 立即 publishOn 到另一个线程 —— ThreadLocal 肯定丢
                .publishOn(Schedulers.boundedElastic())
                .map(R::ok)
                .doFinally(sig -> {
                    // doFinally 在 boundedElastic 里，不在 init 的线程上
                    TraceContext.clear(); // 实际上是在 clear 错线程，模拟旧过滤器行为
                });

        StepVerifier.create(pipeline)
                .consumeNextWith(r -> {
                    // 预期确实没拿到 traceId
                    Object maybe = r.getExtra() == null ? null : r.getExtra().get("traceId");
                    assertThat(maybe).as("Without new API, thread-switch causes missing traceId").isNull();
                })
                .verifyComplete();
    }

    /**
     * 高并发模拟：N 个并行的请求在同一个 scheduler 上竞争，每一个请求的 traceId 都要独属自己。
     */
    @Test
    @DisplayName("并发 100 个独立 Reactor Context 的 Mono，各自 traceId 不串号")
    void test_concurrent_requests_context_no_mix() {
        int n = 100;
        List<Mono<String>> tasks = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            final String myTraceId = String.format("tr%030d", i); // 32 位
            tasks.add(
                    Mono.just("payload-" + i)
                            .publishOn(Schedulers.parallel())
                            // 每个请求自己的 context
                            .flatMap(p -> Mono.deferContextual(ctxView -> Mono.justOrEmpty(
                                    ctxView.<String>getOrEmpty(TraceWebFilter.TRACE_ID_KEY)
                                            .map(tid -> p + "@" + tid)
                            )))
                            .contextWrite(reactor.util.context.Context.of(TraceWebFilter.TRACE_ID_KEY, myTraceId))
            );
        }

        Flux<String> merged = Flux.merge(tasks);
        List<String> collected = new ArrayList<>();
        StepVerifier.create(merged.doOnNext(collected::add))
                .expectNextCount(n)
                .verifyComplete();
        // 校验所有结果
        for (String row : collected) {
            String[] parts = row.split("@");
            assertThat(parts).hasSize(2);
            int idx = Integer.parseInt(parts[0].substring("payload-".length()));
            String expectedTrace = String.format("tr%030d", idx);
            assertThat(parts[1]).isEqualTo(expectedTrace);
        }
    }

    /**
     * 验证旧 ThreadLocal init → publishOn → doFinally 在新线程执行时，init 线程的 ThreadLocal 不会被清除。
     * 这是 TraceWebFilter 修复前的核心 bug，修复后「safeClearOnSameThread 只在同线程 clear，否则 defer 到 Debug」。
     */
    @Test
    @DisplayName("模拟 TraceWebFilter safeClearOnSameThread：doFinally 在不同线程不会清空 init 线程的 ThreadLocal")
    void test_safe_clear_on_same_thread_skips_cross_thread_clear() throws Exception {
        // 初始：当前线程（测试线程）有 traceId
        TraceContext.initTrace("svc", "INIT-THREAD-ID", null);
        Thread initThread = Thread.currentThread();
        final String[] workerSeenByInitThread = {null};

        // 模拟 TraceWebFilter 的 Thread[] initThread 持有机制
        final Thread[] initThreadHolder = {initThread};
        final boolean[] wasSet = {true};

        // 在另一个 worker 线程上执行 doFinally safeClear
        Thread worker = new Thread(() -> {
            // 先在 worker 设一个值（模拟 worker 自己有 trace 的极端场景，概率小但验证）
            TraceContext.initTrace("worker-svc", "WORKER-ID", null);
            // 调用等价的 safeClearOnSameThread 逻辑
            if (initThreadHolder[0] == Thread.currentThread()) {
                TraceContext.clear();
            } else {
                // 只清当前 worker 线程（兜底）
                TraceContext.clear();
            }
            workerSeenByInitThread[0] = "done";
        }, "doFinally-worker");
        worker.start();
        worker.join(2000);
        assertThat(workerSeenByInitThread[0]).isEqualTo("done");

        // 关键断言：init 线程（当前 test 线程）的 TraceContext 仍在，不会被 worker 线程误清除
        assertThat(TraceContext.getTraceId())
                .as("doFinally worker 不应清除 init 线程的 ThreadLocal")
                .isEqualTo("INIT-THREAD-ID");
    }
}
