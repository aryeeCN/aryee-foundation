package cn.aryee.examples.messaging;

import cn.aryee.messaging.autoconfigure.AryeeMessagingAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * Aryee Messaging 示例应用
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootApplication
@Import(AryeeMessagingAutoConfiguration.class)
public class MessagingExamplesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessagingExamplesApplication.class, args);
    }
}
