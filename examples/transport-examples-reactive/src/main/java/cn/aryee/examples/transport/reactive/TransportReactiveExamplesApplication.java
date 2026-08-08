package cn.aryee.examples.transport.reactive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aryee Transport Reactive 模式示例应用启动类
 * 验证 transport-reactive-spring-boot-starter（WebFlux + WebClient）的集成
 *
 * <p>依赖说明：
 * <ul>
 *   <li>引入 transport-reactive-spring-boot-starter（Reactive Starter）</li>
 *   <li>禁止同时引入 transport-spring-boot-starter（架构规则 6.1 Starter 隔离）</li>
 * </ul>
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootApplication
public class TransportReactiveExamplesApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransportReactiveExamplesApplication.class, args);
    }
}
