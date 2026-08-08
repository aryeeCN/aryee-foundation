package cn.aryee.examples.messaging.rocketmq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aryee Messaging RocketMQ Blocking 模式示例应用启动类
 * 用于验证 aryee-foundation-messaging 模块 RocketMQ Blocking 模式的功能
 *
 * <p>依赖说明：
 * <ul>
 *   <li>仅引入 messaging-spring-boot-starter（Blocking Starter）</li>
 *   <li>禁止同时引入 messaging-reactive-spring-boot-starter（架构规则 6.1 Starter 隔离）</li>
 * </ul>
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootApplication
public class MessagingRocketMQExamplesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessagingRocketMQExamplesApplication.class, args);
    }
}
