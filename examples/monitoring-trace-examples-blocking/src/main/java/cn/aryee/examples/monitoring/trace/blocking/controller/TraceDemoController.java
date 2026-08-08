package cn.aryee.examples.monitoring.trace.blocking.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.aryee.commons.response.R;

/**
 * 示例 Controller：返回 R<T>，不手动调用 withTraceId()，验证 traceId 是否自动注入
 *
 * @author Aryee
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api")
public class TraceDemoController {

    @GetMapping("/hello")
    public R<String> hello() {
        // 注意：没有 .withTraceId()，traceId 应该由 R.restResult() 自动填充
        return R.ok("hello aryee")
                .addExtra("demo", "trace-filter-blocking");
    }

    /**
     * 直接返回当前请求的 traceId（从 TraceContext 中获取）
     */
    @GetMapping("/trace-id")
    public R<String> currentTraceId() {
        String traceId = cn.aryee.commons.context.TraceContext.getTraceId();
        return R.ok(traceId);
    }
}
