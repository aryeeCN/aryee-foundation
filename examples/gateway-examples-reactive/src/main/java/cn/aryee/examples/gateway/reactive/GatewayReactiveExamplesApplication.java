package cn.aryee.examples.gateway.reactive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Gateway Reactive 示例应用启动类
 * 演示响应式网关功能：限流、熔断、灰度发布、API 聚合
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootApplication
public class GatewayReactiveExamplesApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayReactiveExamplesApplication.class, args);
    }
}
