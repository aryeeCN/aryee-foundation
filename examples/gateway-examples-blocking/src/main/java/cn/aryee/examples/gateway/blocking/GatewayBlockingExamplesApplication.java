package cn.aryee.examples.gateway.blocking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Gateway Blocking 示例应用启动类
 * 演示限流、熔断、灰度发布、API 聚合等网关功能
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootApplication
public class GatewayBlockingExamplesApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayBlockingExamplesApplication.class, args);
    }
}
