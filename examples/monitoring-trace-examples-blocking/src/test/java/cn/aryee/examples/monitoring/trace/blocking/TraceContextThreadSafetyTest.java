package cn.aryee.examples.monitoring.trace.blocking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cn.aryee.commons.context.TraceContext;

/**
 * 多线程 / 线程池场景下 TraceContext 传递的竞态风险专项测试
 *
 * <p>测试要点：</p>
 * <ol>
 *   <li>wrapRunnable / wrapCallable 正确从调用方线程复制 traceId 到执行线程</li>
 *   <li>线程池复用（线程名字固定 1..N）下，不会出现「请求 A 的 traceId 被 B 读到」</li>
 *   <li>装饰后的 Runnable 执行完毕后，会正确「恢复执行线程原上下文」（不会残留脏数据）</li>
 *   <li>并发 200 线程同时竞争，不会出现 traceId 串号</li>
 *   <li>SpringTraceTaskDecorator.asSpringDecorator 若 Spring 不在类路径不会抛错在 decorate 调用上（实际 Spring 类路径在 test 下存在，因此会验证返回的代理实现 decorate 功能）</li>
 * </ol>
 */
public class TraceContextThreadSafetyTest {

    private ExecutorService pool;
    // 固定线程名以便验证复用
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(1);
    private static final ThreadFactory FIXED_NAMED = r -> {
        Thread t = new Thread(r, "worker-" + THREAD_COUNTER.getAndIncrement());
        t.setDaemon(true);
        return t;
    };

    @BeforeEach
    void setUp() {
        // 故意只用 2 个线程，最大化「线程被下一个任务复用」的概率，暴露残留风险
        pool = Executors.newFixedThreadPool(2, FIXED_NAMED);
    }

    @AfterEach
    void tearDown() {
        pool.shutdownNow();
        TraceContext.clear();
    }

    /**
     * 风险场景1:
     * Controller 线程有 TraceContext，提交 Runnable 到线程池 → 线程池线程取不到 traceId（未包装前的默认行为）
     * → 用 wrapRunnable 后，执行线程应能拿到完全一样的 traceId。
     */
    @Test
    @DisplayName("wrapRunnable 能正确将调用方的 traceId 透传到线程池执行线程")
    void test_wrapRunnable_passes_traceId_to_worker_thread() throws Exception {
        TraceContext.initTrace("demo-svc", "TRACE-A", "SPAN-A");
        final String[] seen = new String[1];
        Future<?> f = pool.submit(TraceContext.wrapRunnable(() -> seen[0] = TraceContext.getTraceId()));
        f.get();
        assertThat(seen[0]).isEqualTo("TRACE-A");
    }

    @Test
    @DisplayName("wrapCallable 能正确将调用方的 traceId 透传到线程池执行线程，并返回业务结果")
    void test_wrapCallable_passes_traceId_and_returns_value() throws Exception {
        TraceContext.initTrace("demo-svc", "TRACE-B", "SPAN-B");
        Callable<String> c = TraceContext.wrapCallable(() -> {
            TraceContext.TraceInfo info = TraceContext.getCurrentTrace();
            return TraceContext.getTraceId() + "::" + (info == null ? null : info.getServiceName());
        });
        Future<String> f = pool.submit(c);
        assertThat(f.get()).isEqualTo("TRACE-B::demo-svc");
    }

    /**
     * 风险场景2（串号）：
     * 线程池 2 个线程，循环提交 200 个任务，每个任务都有独立的 traceId（t1/t2/t3...）。
     * 如果 Runnable 包装后不 restore prevLocal，就会出现「worker-1 上一个任务执行完后，
     * ThreadLocal 还保留 t1，下一个任务 tN 读到 t1」。
     */
    @Test
    @DisplayName("高并发线程池复用场景：wrapRunnable 不会出现 traceId 串号")
    void test_no_cross_contamination_on_thread_reuse() throws Exception {
        int total = 200;
        CountDownLatch latch = new CountDownLatch(total);
        List<Future<String[]>> futures = new ArrayList<>(total);

        for (int i = 0; i < total; i++) {
            final String expectedId = "REQ-" + i;
            // 在"调用方线程"（即 junit 线程）连续切换 initTrace，每次 submit 都 snapshot 一份
            TraceContext.initTrace("svc", expectedId, "span-of-" + i);
            final String snapshotBeforeSubmit = TraceContext.getTraceId();
            Callable<String[]> task = TraceContext.wrapCallable(() -> {
                String seenId = TraceContext.getTraceId();
                String worker = Thread.currentThread().getName();
                latch.countDown();
                return new String[]{snapshotBeforeSubmit, seenId, worker};
            });
            futures.add(pool.submit(task));
        }
        latch.await();
        // 断言所有任务"看到的 id == submit 时捕获的 id"，不存在串号
        for (Future<String[]> fu : futures) {
            String[] row = fu.get();
            assertThat(row[1])
                    .as("Expected submitted=%s, but worker=%s saw=%s (worker thread=%s)",
                            row[0], row[0], row[1], row[2])
                    .isEqualTo(row[0]);
        }
    }

    /**
     * 风险场景3：Runnable 包装后执行结束，应把执行线程"原有的" ThreadLocal 恢复回来。
     * 比如 worker-1 执行任务前就已经 set 了 TraceContext.X（由前一个任务残留的可能性），
     * 包装器要保证 run 结束后把它恢复。
     */
    @Test
    @DisplayName("wrapRunnable 执行结束后恢复执行线程原上下文，避免残留")
    void test_wrapRunnable_restores_worker_thread_previous_context() throws Exception {
        // 先让 worker-1 跑一个"遗留"上下文（模拟没清干净的极端场景）
        CountDownLatch captureName = new CountDownLatch(1);
        final String[] workerThreadName = new String[1];
        pool.submit((Runnable) () -> {
            TraceContext.initTrace("legacy", "LEGACY-ID", null);
            workerThreadName[0] = Thread.currentThread().getName();
            captureName.countDown();
        }).get();
        captureName.await();

        // 现在 submit 一个 traceId=NEW-ID 的包装任务，复用同一个 worker 的概率非常高（pool=1 个线程可强制）
        // 为了 100% 复用，临时换单线程池
        ExecutorService singlePool = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "single-worker");
            t.setDaemon(true);
            return t;
        });
        try {
            // stage 1: single-worker 先遗留 LEGACY
            singlePool.submit((Runnable) () -> TraceContext.initTrace("legacy", "LEGACY-ID", null)).get();
            // stage 2: 在 junit 主线程 set NEW，提交包装任务
            TraceContext.initTrace("new", "NEW-ID", null);
            String[] insideSeen = new String[1];
            singlePool.submit(TraceContext.wrapRunnable(() -> insideSeen[0] = TraceContext.getTraceId())).get();
            assertThat(insideSeen[0]).isEqualTo("NEW-ID");
            // stage 3: 在 single-worker 里再直接跑一个"没包装"的任务，应该看到之前 stage1 的 LEGACY（因为包装器 restore 了）
            String[] afterSeen = new String[1];
            singlePool.submit((Runnable) () -> afterSeen[0] = TraceContext.getTraceId()).get();
            assertThat(afterSeen[0]).isEqualTo("LEGACY-ID");
        } finally {
            singlePool.shutdownNow();
        }
    }

    /**
     * 风险场景4: junit 主线程本就是"空 traceId"，submit 后 worker 线程也应该是空，不应莫名其妙读到别的值。
     */
    @Test
    @DisplayName("调用方无线程上下文时 wrapRunnable 不会捏造 traceId")
    void test_wrapRunnable_when_no_context_keeps_worker_clean() throws Exception {
        TraceContext.clear();
        assertThat(TraceContext.getTraceId()).isNull();
        String[] seen = new String[1];
        pool.submit(TraceContext.wrapRunnable(() -> seen[0] = TraceContext.getTraceId())).get();
        assertThat(seen[0]).isNull();
    }

    @Test
    @DisplayName("getOrCreateTraceId 在无上下文时自动生成并写入")
    void test_getOrCreateTraceId_bootstrap_when_missing() {
        TraceContext.clear();
        String id = TraceContext.getOrCreateTraceId();
        assertThat(id).isNotNull().isNotEmpty();
        assertThat(TraceContext.getTraceId()).isEqualTo(id);
        assertThat(id).hasSize(32); // UUID 无横线
    }

    @Test
    @DisplayName("SpringTraceTaskDecorator.decorate 直接调用 = wrapRunnable（不依赖 Spring 类加载）")
    void test_spring_trace_task_decorator_direct_api() {
        TraceContext.initTrace("demo-svc", "DECO-A", null);
        TraceContext.SpringTraceTaskDecorator d = new TraceContext.SpringTraceTaskDecorator();
        Runnable wrapped = d.decorate(() -> {
            // 断言执行线程里拿到 DECO-A
            assertThat(TraceContext.getTraceId()).isEqualTo("DECO-A");
        });
        // 单独在当前线程跑（当前已经有 DECO-A，装饰后再设置一次还是 DECO-A；验证没报错）
        assertThatCode(wrapped::run).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SpringTraceTaskDecorator.asSpringDecorator 反射代理 decorate 功能正常（Spring 在 test classpath 上）")
    void test_asSpringDecorator_reflective_proxy_works() {
        Object decorator = TraceContext.SpringTraceTaskDecorator.asSpringDecorator();
        // Spring 的 TaskDecorator 接口在 classpath 上（Spring Boot test 会引入 spring-core）
        assertThat(decorator).isInstanceOf(org.springframework.core.task.TaskDecorator.class);
        org.springframework.core.task.TaskDecorator td = (org.springframework.core.task.TaskDecorator) decorator;

        TraceContext.initTrace("demo-svc", "PROXY-A", null);
        final String[] seen = new String[1];
        Runnable decorated = td.decorate(() -> seen[0] = TraceContext.getTraceId());
        // 让 decorated 在另一个线程跑，验证代理的 decorate 底层实际调用了 wrapRunnable
        new Thread(decorated, "t-decorator-proxy").start();
        // 等最多 2s
        long end = System.currentTimeMillis() + 2000;
        while (seen[0] == null && System.currentTimeMillis() < end) {
            Thread.yield();
        }
        assertThat(seen[0]).isEqualTo("PROXY-A");
    }
}
