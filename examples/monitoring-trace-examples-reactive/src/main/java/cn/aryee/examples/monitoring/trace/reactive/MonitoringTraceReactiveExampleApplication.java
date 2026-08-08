package cn.aryee.examples.monitoring.trace.reactive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 链路追踪示例 - Reactive (WebFlux) 模式
 *
 * <pre>
 *   # 启动
 *   mvn spring-boot:run
 *   # 发请求验证
 *   curl -H "traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01" http://localhost:8082/api/hello -v
 *   # 单元测试
 *   mvn test
 * </pre>
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootApplication
public class MonitoringTraceReactiveExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonitoringTraceReactiveExampleApplication.class, args);
    }
}
