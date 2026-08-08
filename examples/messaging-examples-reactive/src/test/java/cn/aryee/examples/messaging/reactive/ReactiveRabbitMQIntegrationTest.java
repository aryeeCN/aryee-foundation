package cn.aryee.examples.messaging.reactive;

import cn.aryee.messaging.api.model.Message;
import cn.aryee.messaging.api.model.MessageBuilder;
import cn.aryee.messaging.api.model.MessageModel;
import cn.aryee.messaging.api.service.ReactiveMessagePublisher;
import cn.aryee.messaging.infrastructure.reactive.rabbitmq.ReactiveRabbitMQMessagePublisher;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;
import reactor.rabbitmq.Sender;
import reactor.rabbitmq.SenderOptions;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Aryee Messaging Reactive RabbitMQ 集成测试
 * 验证 ReactiveRabbitMQMessagePublisher 与真实 RabbitMQ 服务的连通性和功能正确性
 *
 * <p>架构规则 6.1 安全隔离原则：
 * <ul>
 *   <li>仅引入 messaging-reactive-spring-boot-starter</li>
 *   <li>禁止同时引入 messaging-spring-boot-starter（Blocking Starter）</li>
 *   <li>测试使用 reactor-rabbitmq 的 Sender（非阻塞）</li>
 * </ul>
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootTest
@Import(ReactiveRabbitMQIntegrationTest.TestConfig.class)
@DisplayName("Reactive RabbitMQ 消息集成测试")
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ReactiveRabbitMQIntegrationTest {

    private final ReactiveMessagePublisher reactiveMessagePublisher;

    private static final String TEST_TOPIC = "test.topic.reactive";

    @Configuration
    static class TestConfig {

        @Bean
        public Sender sender() {
            com.rabbitmq.client.ConnectionFactory factory = new com.rabbitmq.client.ConnectionFactory();
            factory.setHost("localhost");
            factory.setPort(5672);
            factory.setUsername("admin");
            factory.setPassword("a123456");
            factory.setVirtualHost("/");
            return new Sender(new SenderOptions().connectionFactory(factory));
        }

        @Bean
        public ReactiveMessagePublisher reactiveMessagePublisher(Sender sender) {
            return new ReactiveRabbitMQMessagePublisher(sender);
        }
    }

    @BeforeEach
    void setUp() {
        assertThat(reactiveMessagePublisher).isNotNull();
    }

    @Test
    @DisplayName("1. 验证 RabbitMQ 响应式连接健康检查")
    void testHealthCheck() {
        StepVerifier.create(reactiveMessagePublisher.isHealthy())
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("2. 基本响应式消息发布 - publish(Message)")
    void testBasicPublish() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello Reactive RabbitMQ - Basic Publish")
                .build();

        StepVerifier.create(reactiveMessagePublisher.publish(message))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("3. 使用 MessageBuilder 构建并发布响应式消息")
    void testPublishWithBuilder() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello Reactive RabbitMQ - Builder Pattern")
                .source("test-reactive-app")
                .priority(cn.aryee.messaging.api.model.MessagePriority.HIGH)
                .header("test-header", "reactive-value")
                .build();

        StepVerifier.create(reactiveMessagePublisher.publish(message))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("4. 带路由键响应式发布 - publish(message, topic, routingKey)")
    void testPublishWithRoutingKey() {
        // 使用默认 exchange（空字符串）
        Message message = MessageBuilder.create()
                .textPayload("Hello Reactive RabbitMQ - With Routing Key")
                .build();

        StepVerifier.create(reactiveMessagePublisher.publish(message, "", "test.key.reactive"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("5. 带请求头响应式发布 - publishWithHeaders")
    void testPublishWithHeaders() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello Reactive RabbitMQ - With Headers")
                .build();

        Map<String, Object> headers = new HashMap<>();
        headers.put("x-test-header", "reactive-value");
        headers.put("x-message-type", "reactive-test");

        StepVerifier.create(reactiveMessagePublisher.publishWithHeaders(message, headers))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("6. 异步响应式消息发布 - publishAsync")
    void testAsyncPublish() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello Reactive RabbitMQ - Async Publish")
                .build();

        StepVerifier.create(reactiveMessagePublisher.publishAsync(message))
                .verifyComplete();
    }

    @Test
    @DisplayName("7. 异步响应式 Future 消息发布 - publishAsyncFuture")
    void testAsyncFuturePublish() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello Reactive RabbitMQ - Async Future")
                .build();

        StepVerifier.create(reactiveMessagePublisher.publishAsyncFuture(message))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("8. 批量响应式消息发布 - publishBatch(List)")
    void testBatchPublish() {
        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            messages.add(MessageBuilder.create(TEST_TOPIC)
                    .textPayload("Reactive Batch Message " + i)
                    .build());
        }

        StepVerifier.create(reactiveMessagePublisher.publishBatch(messages))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("9. 事务响应式消息发布 - publishTransactional")
    void testTransactionalPublish() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello Reactive RabbitMQ - Transactional")
                .build();

        StepVerifier.create(reactiveMessagePublisher.publishTransactional(message))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("10. JSON 响应式消息发布")
    void testJsonMessagePublish() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", 1001);
        payload.put("username", "Aryee");
        payload.put("action", "reactive-test");

        Message message = MessageBuilder.create(TEST_TOPIC)
                .payload(payload)
                .build();

        StepVerifier.create(reactiveMessagePublisher.publish(message))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("11. 发送到指定 Topic - publish(message, topic)")
    void testPublishToSpecificTopic() {
        Message message = MessageBuilder.create()
                .textPayload("Hello Reactive RabbitMQ - Specific Topic")
                .build();

        StepVerifier.create(reactiveMessagePublisher.publish(message, TEST_TOPIC))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("12. MessageModel 响应式发布")
    void testMessageModelPublish() {
        MessageModel<Void> messageModel = new MessageModel<>();
        messageModel.setTopic(TEST_TOPIC);
        messageModel.setPayload("Hello Reactive RabbitMQ - MessageModel");

        StepVerifier.create(reactiveMessagePublisher.publish(messageModel))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("13. 获取响应式发布器名称")
    void testGetPublisherName() {
        String publisherName = reactiveMessagePublisher.getPublisherName();
        assertThat(publisherName).isEqualTo("ReactiveRabbitMQMessagePublisher");
    }

    @Test
    @DisplayName("14. 发送同步确认响应式消息 - sendSync")
    void testSendSync() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello Reactive RabbitMQ - Sync Message")
                .build();

        StepVerifier.create(reactiveMessagePublisher.sendSync(message))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("15. 延迟响应式消息发布 - publishDelayed")
    void testPublishDelayed() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello Reactive RabbitMQ - Delayed")
                .build();

        StepVerifier.create(reactiveMessagePublisher.publishDelayed(message, 100L))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("16. 批量响应式消息流发布 - publishBatchFlux")
    void testPublishBatchFlux() {
        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            messages.add(MessageBuilder.create(TEST_TOPIC)
                    .textPayload("Reactive Flux Message " + i)
                    .build());
        }

        StepVerifier.create(reactiveMessagePublisher.publishBatchFlux(messages))
                .expectNext(true, true, true)
                .verifyComplete();
    }
}
