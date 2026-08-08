package cn.aryee.examples.monitoring.trace.blocking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 链路追踪示例 - Blocking 模式
 * <p>
 * 目标：验证 TraceFilter 能从请求头中提取 traceId（traceparent / X-B3-TraceId / x-trace-id），
 * 并把 traceId 写入：
 * 1) HTTP 响应头 x-trace-id
 * 2) 统一响应 R<T> 的 extra.traceId
 * </p>
 *
 * <h2>使用方式</h2>
 * <pre>
 *   # 1. 启动示例应用
 *   mvn spring-boot:run
 *
 *   # 2. 发送带 traceparent 请求头的请求（W3C TraceContext 格式：version-traceId32-spanId16-flags）
 *   curl -H "traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01" \
 *        http://localhost:8081/api/hello
 *
 *   # 3. 查看响应头（应包含 x-trace-id: 4bf92f3577b34da6a3ce929d0e0e4736）
 *
 *   # 4. 用 B3 头请求（Zipkin 兼容）
 *   curl -H "X-B3-TraceId: 8a3c40a79a1e4c72bf1e5c9a8d3b2f1e" \
 *        -H "X-B3-SpanId: 00f067aa0ba902b7" http://localhost:8081/api/hello
 *
 *   # 5. 运行单测（MockMvc 发请求自动化验证所有场景）
 *   mvn test
 * </pre>
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootApplication
public class MonitoringTraceBlockingExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonitoringTraceBlockingExampleApplication.class, args);
    }
}
