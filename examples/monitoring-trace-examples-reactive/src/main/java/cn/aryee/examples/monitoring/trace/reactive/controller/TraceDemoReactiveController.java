package cn.aryee.examples.monitoring.trace.reactive.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.aryee.commons.response.R;
import reactor.core.publisher.Mono;

/**
 * Reactive 示例 Controller
 *
 * @author Aryee
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api")
public class TraceDemoReactiveController {

    @GetMapping("/hello")
    public Mono<R<String>> hello() {
        // 不手动调用 withTraceId()，验证由 R.restResult() 自动填充 + Reactor Context 里的 traceId 生效
        return Mono.just(R.ok("hello aryee reactive")
                .addExtra("demo", "trace-filter-reactive"));
    }

    @GetMapping("/trace-id")
    public Mono<R<String>> currentTraceId() {
        // Reactive 模式：请求入口线程 TraceContext.ThreadLocal 由 TraceWebFilter 初始化后，能直接读到
        String traceId = cn.aryee.commons.context.TraceContext.getTraceId();
        return Mono.just(R.ok(traceId));
    }
}
