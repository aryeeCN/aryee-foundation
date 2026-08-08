package cn.aryee.examples.messaging.kafka;

import cn.aryee.messaging.api.model.Message;
import cn.aryee.messaging.api.model.MessageBuilder;
import cn.aryee.messaging.api.model.MessageModel;
import cn.aryee.messaging.api.service.MessagePublisher;
import cn.aryee.messaging.infrastructure.blocking.kafka.KafkaMessagePublisher;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Aryee Messaging Kafka Blocking 集成测试
 * 验证 KafkaMessagePublisher 与真实 Kafka 服务的连通性和功能正确性
 *
 * <p>架构规则 6.1 安全隔离原则：
 * <ul>
 *   <li>仅引入 messaging-spring-boot-starter（Blocking Starter）</li>
 *   <li>禁止同时引入 messaging-reactive-spring-boot-starter（Reactive Starter）</li>
 * </ul>
 *
 * @author Aryee
 * @since 1.0.0
 */
@DisplayName("Kafka Blocking 消息集成测试")
class KafkaIntegrationTest {

    private static MessagePublisher messagePublisher;

    private static final String TEST_TOPIC = "test-topic-blocking";

    @BeforeAll
    static void setUp() {
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.RETRIES_CONFIG, 3);

        Map<String, Object> producerConfig = new HashMap<String, Object>();
        producerProps.forEach((key, value) -> producerConfig.put((String) key, value));
        ProducerFactory<String, Object> producerFactory = new DefaultKafkaProducerFactory<String, Object>(producerConfig);
        KafkaTemplate<String, Object> kafkaTemplate = new KafkaTemplate<>(producerFactory);

        messagePublisher = new KafkaMessagePublisher(kafkaTemplate);
    }

    @Test
    @DisplayName("1. 验证 Kafka 连接健康检查")
    void testHealthCheck() {
        boolean healthy = messagePublisher.isHealthy();
        assertThat(healthy).isTrue();
    }

    @Test
    @DisplayName("2. 基本消息发布 - publish(Message)")
    void testBasicPublish() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello Kafka - Basic Publish")
                .build();

        boolean result = messagePublisher.publish(message);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("3. 使用 MessageBuilder 构建并发布消息")
    void testPublishWithBuilder() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello Kafka - Builder Pattern")
                .source("test-app")
                .priority(cn.aryee.messaging.api.model.MessagePriority.HIGH)
                .header("test-header", "test-value")
                .build();

        boolean result = messagePublisher.publish(message);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("4. 带路由键发布 - publish(message, topic, routingKey)")
    void testPublishWithRoutingKey() {
        Message message = MessageBuilder.create()
                .textPayload("Hello Kafka - With Routing Key")
                .build();

        boolean result = messagePublisher.publish(message, TEST_TOPIC, "test-key");
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("5. 带请求头发布 - publishWithHeaders")
    void testPublishWithHeaders() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello Kafka - With Headers")
                .build();

        Map<String, Object> headers = new HashMap<>();
        headers.put("x-test-header", "test-value");
        headers.put("x-message-type", "test");

        boolean result = messagePublisher.publishWithHeaders(message, headers);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("6. 异步消息发布 - publishAsync")
    void testAsyncPublish() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello Kafka - Async Publish")
                .build();

        messagePublisher.publishAsync(message);
        // 异步发送不阻塞，验证不抛异常即可
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("7. 异步 Future 消息发布 - publishAsyncFuture")
    void testAsyncFuturePublish() throws Exception {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello Kafka - Async Future")
                .build();

        Boolean result = messagePublisher.publishAsyncFuture(message).get(10, TimeUnit.SECONDS);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("8. 批量消息发布 - publishBatch(List)")
    void testBatchPublish() {
        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            messages.add(MessageBuilder.create(TEST_TOPIC)
                    .textPayload("Batch Message " + i)
                    .build());
        }

        boolean result = messagePublisher.publishBatch(messages);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("9. 事务消息发布 - publishTransactional")
    void testTransactionalPublish() {
        // Kafka 事务需要 transactional.id 支持，使用独立的事务生产者
        Properties transactionalProps = new Properties();
        transactionalProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        transactionalProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        transactionalProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        transactionalProps.put(ProducerConfig.ACKS_CONFIG, "all");
        transactionalProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "test-transactional-id-" + System.currentTimeMillis());

        Map<String, Object> txConfig = new HashMap<String, Object>();
        transactionalProps.forEach((key, value) -> txConfig.put((String) key, value));
        ProducerFactory<String, Object> txProducerFactory = new DefaultKafkaProducerFactory<String, Object>(txConfig);
        KafkaTemplate<String, Object> txKafkaTemplate = new KafkaTemplate<>(txProducerFactory);
        KafkaMessagePublisher txPublisher = new KafkaMessagePublisher(txKafkaTemplate);

        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello Kafka - Transactional")
                .build();

        boolean result = txPublisher.publishTransactional(message);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("10. JSON 消息发布")
    void testJsonMessagePublish() {
        String jsonPayload = "{\"userId\":1001,\"username\":\"Aryee\",\"action\":\"test\"}";

        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload(jsonPayload)
                .build();

        boolean result = messagePublisher.publish(message);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("11. 发送到指定 Topic - publish(message, topic)")
    void testPublishToSpecificTopic() {
        Message message = MessageBuilder.create()
                .textPayload("Hello Kafka - Specific Topic")
                .build();

        boolean result = messagePublisher.publish(message, TEST_TOPIC);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("12. MessageModel 发布")
    void testMessageModelPublish() {
        MessageModel<Void> messageModel = new MessageModel<>();
        messageModel.setTopic(TEST_TOPIC);
        messageModel.setPayload("Hello Kafka - MessageModel");

        boolean result = messagePublisher.publish(messageModel);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("13. 获取发布器名称")
    void testGetPublisherName() {
        String publisherName = messagePublisher.getPublisherName();
        assertThat(publisherName).isEqualTo("KafkaMessagePublisher");
    }

    @Test
    @DisplayName("14. 发送同步确认消息 - sendSync")
    void testSendSync() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello Kafka - Sync Message")
                .build();

        boolean result = messagePublisher.sendSync(message);
        assertThat(result).isTrue();
    }
}
