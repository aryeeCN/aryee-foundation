package cn.aryee.examples.messaging.rocketmq.reactive;

import cn.aryee.messaging.api.model.Message;
import cn.aryee.messaging.api.model.MessageBuilder;
import cn.aryee.messaging.api.model.MessageModel;
import cn.aryee.messaging.api.service.ReactiveMessagePublisher;
import cn.aryee.messaging.infrastructure.reactive.rocketmq.ReactiveRocketMQMessagePublisher;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Aryee Messaging RocketMQ Reactive 集成测试
 * 验证 ReactiveRocketMQMessagePublisher 与真实 RocketMQ 服务的连通性和功能正确性
 *
 * <p>架构规则 6.1 安全隔离原则：
 * <ul>
 *   <li>仅引入 messaging-reactive-spring-boot-starter</li>
 *   <li>禁止同时引入 messaging-spring-boot-starter（Blocking Starter）</li>
 *   <li>Reactive 实现使用 boundedElastic 调度器包装阻塞调用</li>
 * </ul>
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootTest(properties = "spring.autoconfigure.exclude=org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration")
@Import(ReactiveRocketMQIntegrationTest.TestConfig.class)
@DisplayName("RocketMQ Reactive 消息集成测试")
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ReactiveRocketMQIntegrationTest {

    private final ReactiveMessagePublisher reactiveMessagePublisher;

    private static final String TEST_TOPIC = "test-topic-reactive";

    @Configuration
    static class TestConfig {

        @Bean(destroyMethod = "shutdown")
        public DefaultMQProducer defaultMQProducer() {
            DefaultMQProducer producer = new DefaultMQProducer("aryee-messaging-reactive-test-producer");
            producer.setNamesrvAddr("localhost:9876");
            producer.setSendMsgTimeout(30000);
            return producer;
        }

        @Bean
        public RocketMQTemplate rocketMQTemplate(DefaultMQProducer producer) {
            RocketMQTemplate template = new RocketMQTemplate();
            template.setProducer(producer);
            return template;
        }

        @Bean
        public ReactiveMessagePublisher reactiveMessagePublisher(RocketMQTemplate rocketMQTemplate) {
            return new ReactiveRocketMQMessagePublisher(rocketMQTemplate);
        }
    }

    @BeforeEach
    void setUp() {
        assertThat(reactiveMessagePublisher).isNotNull();
    }

    @Test
    @DisplayName("1. 验证 RocketMQ 响应式连接健康检查")
    void testHealthCheck() {
        StepVerifier.create(reactiveMessagePublisher.isHealthy())
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("2. 基本响应式消息发布 - publish(Message)")
    void testBasicPublish() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello Reactive RocketMQ - Basic Publish")
                .build();

        StepVerifier.create(reactiveMessagePublisher.publish(message))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("3. 使用 MessageBuilder 构建并发布响应式消息")
    void testPublishWithBuilder() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello Reactive RocketMQ - Builder Pattern")
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
        Message message = MessageBuilder.create()
                .textPayload("Hello Reactive RocketMQ - With Routing Key")
                .build();

        // RocketMQ 中 routingKey 作为 tag 使用
        StepVerifier.create(reactiveMessagePublisher.publish(message, TEST_TOPIC, "test-tag"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("5. 带请求头响应式发布 - publishWithHeaders")
    void testPublishWithHeaders() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello Reactive RocketMQ - With Headers")
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
                .textPayload("Hello Reactive RocketMQ - Async Publish")
                .build();

        StepVerifier.create(reactiveMessagePublisher.publishAsync(message))
                .verifyComplete();
    }

    @Test
    @DisplayName("7. 批量响应式消息发布 - publishBatch(List)")
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
    @DisplayName("8. 事务响应式消息发布 - publishTransactional")
    void testTransactionalPublish() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello Reactive RocketMQ - Transactional")
                .build();

        StepVerifier.create(reactiveMessagePublisher.publishTransactional(message))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("9. JSON 响应式消息发布")
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
    @DisplayName("10. 发送到指定 Topic - publish(message, topic)")
    void testPublishToSpecificTopic() {
        Message message = MessageBuilder.create()
                .textPayload("Hello Reactive RocketMQ - Specific Topic")
                .build();

        StepVerifier.create(reactiveMessagePublisher.publish(message, TEST_TOPIC))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("11. MessageModel 响应式发布")
    void testMessageModelPublish() {
        MessageModel<Void> messageModel = new MessageModel<>();
        messageModel.setTopic(TEST_TOPIC);
        messageModel.setPayload("Hello Reactive RocketMQ - MessageModel");

        StepVerifier.create(reactiveMessagePublisher.publish(messageModel))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("12. 获取响应式发布器名称")
    void testGetPublisherName() {
        String publisherName = reactiveMessagePublisher.getPublisherName();
        assertThat(publisherName).isEqualTo("ReactiveRocketMQMessagePublisher");
    }

    @Test
    @DisplayName("13. 发送同步确认响应式消息 - sendSync")
    void testSendSync() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello Reactive RocketMQ - Sync Message")
                .build();

        StepVerifier.create(reactiveMessagePublisher.sendSync(message))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("14. 延迟响应式消息发布 - publishDelayed")
    void testPublishDelayed() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello Reactive RocketMQ - Delayed")
                .build();

        StepVerifier.create(reactiveMessagePublisher.publishDelayed(message, 100L))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("15. 批量响应式消息流发布 - publishBatchFlux")
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
