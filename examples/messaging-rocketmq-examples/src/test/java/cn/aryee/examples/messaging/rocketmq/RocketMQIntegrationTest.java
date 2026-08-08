package cn.aryee.examples.messaging.rocketmq;

import cn.aryee.messaging.api.model.Message;
import cn.aryee.messaging.api.model.MessageBuilder;
import cn.aryee.messaging.api.model.MessageModel;
import cn.aryee.messaging.api.service.MessagePublisher;
import cn.aryee.messaging.infrastructure.blocking.rocketmq.RocketMQMessagePublisher;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Aryee Messaging RocketMQ Blocking 集成测试
 * 验证 RocketMQMessagePublisher 与真实 RocketMQ 服务的连通性和功能正确性
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
@SpringBootTest(properties = "spring.autoconfigure.exclude=org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration")
@Import(RocketMQIntegrationTest.TestConfig.class)
@DisplayName("RocketMQ Blocking 消息集成测试")
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class RocketMQIntegrationTest {

    private final MessagePublisher messagePublisher;

    private static final String TEST_TOPIC = "test-topic-blocking";

    @Configuration
    static class TestConfig {

        @Bean(destroyMethod = "shutdown")
        public DefaultMQProducer defaultMQProducer() {
            DefaultMQProducer producer = new DefaultMQProducer("aryee-messaging-test-producer");
            producer.setNamesrvAddr("localhost:9876");
            producer.setSendMsgTimeout(30000);
            // 无需显式 start()：RocketMQTemplate.afterPropertiesSet() 会自动启动 producer
            return producer;
        }

        @Bean
        public RocketMQTemplate rocketMQTemplate(DefaultMQProducer producer) {
            RocketMQTemplate template = new RocketMQTemplate();
            template.setProducer(producer);
            return template;
        }

        @Bean
        public MessagePublisher messagePublisher(RocketMQTemplate rocketMQTemplate) {
            return new RocketMQMessagePublisher(rocketMQTemplate);
        }
    }

    @BeforeEach
    void setUp() {
        assertThat(messagePublisher).isNotNull();
    }

    @Test
    @DisplayName("1. 验证 RocketMQ 连接健康检查")
    void testHealthCheck() {
        boolean healthy = messagePublisher.isHealthy();
        assertThat(healthy).isTrue();
    }

    @Test
    @DisplayName("2. 基本消息发布 - publish(Message)")
    void testBasicPublish() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello RocketMQ - Basic Publish")
                .build();

        boolean result = messagePublisher.publish(message);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("3. 使用 MessageBuilder 构建并发布消息")
    void testPublishWithBuilder() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello RocketMQ - Builder Pattern")
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
                .textPayload("Hello RocketMQ - With Routing Key")
                .build();

        // RocketMQ 中 routingKey 作为 tag 使用
        boolean result = messagePublisher.publish(message, TEST_TOPIC, "test-tag");
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("5. 带请求头发布 - publishWithHeaders")
    void testPublishWithHeaders() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello RocketMQ - With Headers")
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
                .textPayload("Hello RocketMQ - Async Publish")
                .build();

        messagePublisher.publishAsync(message);
        // 异步发送不阻塞，验证不抛异常即可
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("7. 异步 Future 消息发布 - publishAsyncFuture")
    void testAsyncFuturePublish() throws Exception {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello RocketMQ - Async Future")
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
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello RocketMQ - Transactional")
                .build();

        boolean result = messagePublisher.publishTransactional(message);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("10. JSON 消息发布")
    void testJsonMessagePublish() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", 1001);
        payload.put("username", "Aryee");
        payload.put("action", "test");

        Message message = MessageBuilder.create(TEST_TOPIC)
                .payload(payload)
                .build();

        boolean result = messagePublisher.publish(message);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("11. 发送到指定 Topic - publish(message, topic)")
    void testPublishToSpecificTopic() {
        Message message = MessageBuilder.create()
                .textPayload("Hello RocketMQ - Specific Topic")
                .build();

        boolean result = messagePublisher.publish(message, TEST_TOPIC);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("12. MessageModel 发布")
    void testMessageModelPublish() {
        MessageModel<Void> messageModel = new MessageModel<>();
        messageModel.setTopic(TEST_TOPIC);
        messageModel.setPayload("Hello RocketMQ - MessageModel");

        boolean result = messagePublisher.publish(messageModel);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("13. 获取发布器名称")
    void testGetPublisherName() {
        String publisherName = messagePublisher.getPublisherName();
        assertThat(publisherName).isEqualTo("RocketMQMessagePublisher");
    }

    @Test
    @DisplayName("14. 发送同步确认消息 - sendSync")
    void testSendSync() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello RocketMQ - Sync Message")
                .build();

        boolean result = messagePublisher.sendSync(message);
        assertThat(result).isTrue();
    }
}
