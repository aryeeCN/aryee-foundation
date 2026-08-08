package cn.aryee.examples.transport.blocking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Aryee Transport Blocking 模式示例应用启动类
 * 验证 transport-spring-boot-starter（WebMVC + Feign）的集成
 *
 * <p>依赖说明：
 * <ul>
 *   <li>引入 transport-spring-boot-starter（Blocking Starter）</li>
 *   <li>禁止同时引入 transport-reactive-spring-boot-starter（架构规则 6.1 Starter 隔离）</li>
 * </ul>
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootApplication
@EnableFeignClients
public class TransportBlockingExamplesApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransportBlockingExamplesApplication.class, args);
    }
}
