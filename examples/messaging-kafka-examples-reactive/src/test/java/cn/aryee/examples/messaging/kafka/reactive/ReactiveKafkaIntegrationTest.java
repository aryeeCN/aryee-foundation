package cn.aryee.examples.messaging.kafka.reactive;

import cn.aryee.messaging.api.model.Message;
import cn.aryee.messaging.api.model.MessageBuilder;
import cn.aryee.messaging.api.model.MessageModel;
import cn.aryee.messaging.api.service.ReactiveMessagePublisher;
import cn.aryee.messaging.infrastructure.reactive.kafka.ReactiveKafkaMessagePublisher;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Aryee Messaging Kafka Reactive 集成测试
 * 验证 ReactiveKafkaMessagePublisher 与真实 Kafka 服务的连通性和功能正确性
 *
 * <p>架构规则 6.1 安全隔离原则：
 * <ul>
 *   <li>仅引入 messaging-reactive-spring-boot-starter</li>
 *   <li>禁止同时引入 messaging-spring-boot-starter（Blocking Starter）</li>
 *   <li>Reactive 实现使用 reactor-kafka 的 KafkaSender</li>
 * </ul>
 *
 * @author Aryee
 * @since 1.0.0
 */
@DisplayName("Kafka Reactive 消息集成测试")
class ReactiveKafkaIntegrationTest {

    private static ReactiveMessagePublisher reactiveMessagePublisher;

    private static final String TEST_TOPIC = "test-topic-reactive";

    @BeforeAll
    static void setUp() {
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");

        SenderOptions<String, Object> senderOptions = SenderOptions.create(producerProps);
        KafkaSender<String, Object> kafkaSender = KafkaSender.create(senderOptions);
        reactiveMessagePublisher = new ReactiveKafkaMessagePublisher(kafkaSender);
    }

    @Test
    @DisplayName("1. 验证 Kafka 响应式连接健康检查")
    void testHealthCheck() {
        StepVerifier.create(reactiveMessagePublisher.isHealthy())
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("2. 基本响应式消息发布 - publish(Message)")
    void testBasicPublish() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello Reactive Kafka - Basic Publish")
                .build();

        StepVerifier.create(reactiveMessagePublisher.publish(message))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("3. 使用 MessageBuilder 构建并发布响应式消息")
    void testPublishWithBuilder() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello Reactive Kafka - Builder Pattern")
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
                .textPayload("Hello Reactive Kafka - With Routing Key")
                .build();

        StepVerifier.create(reactiveMessagePublisher.publish(message, TEST_TOPIC, "test-key"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("5. 带请求头响应式发布 - publishWithHeaders")
    void testPublishWithHeaders() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello Reactive Kafka - With Headers")
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
                .textPayload("Hello Reactive Kafka - Async Publish")
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
                .textPayload("Hello Reactive Kafka - Transactional")
                .build();

        StepVerifier.create(reactiveMessagePublisher.publishTransactional(message))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("9. JSON 响应式消息发布")
    void testJsonMessagePublish() {
        String jsonPayload = "{\"userId\":1001,\"username\":\"Aryee\",\"action\":\"reactive-test\"}";

        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload(jsonPayload)
                .build();

        StepVerifier.create(reactiveMessagePublisher.publish(message))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("10. 发送到指定 Topic - publish(message, topic)")
    void testPublishToSpecificTopic() {
        Message message = MessageBuilder.create()
                .textPayload("Hello Reactive Kafka - Specific Topic")
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
        messageModel.setPayload("Hello Reactive Kafka - MessageModel");

        StepVerifier.create(reactiveMessagePublisher.publish(messageModel))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("12. 获取响应式发布器名称")
    void testGetPublisherName() {
        String publisherName = reactiveMessagePublisher.getPublisherName();
        assertThat(publisherName).isEqualTo("ReactiveKafkaMessagePublisher");
    }

    @Test
    @DisplayName("13. 发送同步确认响应式消息 - sendSync")
    void testSendSync() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello Reactive Kafka - Sync Message")
                .build();

        StepVerifier.create(reactiveMessagePublisher.sendSync(message))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("14. 延迟响应式消息发布 - publishDelayed")
    void testPublishDelayed() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello Reactive Kafka - Delayed")
                .build();

        StepVerifier.create(reactiveMessagePublisher.publishDelayed(message, 100L))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("15. 有序响应式消息发布 - publishOrdered")
    void testPublishOrdered() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello Reactive Kafka - Ordered")
                .build();

        StepVerifier.create(reactiveMessagePublisher.publishOrdered(message, "order-key-1"))
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
