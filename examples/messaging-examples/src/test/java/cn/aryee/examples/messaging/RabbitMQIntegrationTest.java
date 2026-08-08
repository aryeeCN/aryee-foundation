package cn.aryee.examples.messaging;

import cn.aryee.messaging.api.model.Message;
import cn.aryee.messaging.api.model.MessageBuilder;
import cn.aryee.messaging.api.model.MessageModel;
import cn.aryee.messaging.api.service.MessagePublisher;
import cn.aryee.messaging.infrastructure.blocking.rabbitmq.RabbitMQMessagePublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
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
 * Aryee Messaging RabbitMQ 集成测试
 * 验证 RabbitMQMessagePublisher 与真实 RabbitMQ 服务的连通性和功能正确性
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootTest
@Import(RabbitMQIntegrationTest.TestConfig.class)
@DisplayName("RabbitMQ 消息集成测试")
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class RabbitMQIntegrationTest {

    private final MessagePublisher messagePublisher;

    private final RabbitTemplate rabbitTemplate;

    private static final String TEST_TOPIC = "test.topic";

    @Configuration
    static class TestConfig {

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        public CachingConnectionFactory connectionFactory() {
            CachingConnectionFactory cf = new CachingConnectionFactory("localhost", 5672);
            cf.setUsername("admin");
            cf.setPassword("a123456");
            cf.setVirtualHost("/");
            cf.setConnectionTimeout(30000);
            cf.setChannelCheckoutTimeout(30000);
            cf.afterPropertiesSet();
            return cf;
        }

        @Bean
        public RabbitTemplate rabbitTemplate(CachingConnectionFactory connectionFactory, ObjectMapper objectMapper) {
            RabbitTemplate template = new RabbitTemplate(connectionFactory);
            template.setMessageConverter(new Jackson2JsonMessageConverter(objectMapper));
            return template;
        }

        @Bean
        public MessagePublisher messagePublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
            return new RabbitMQMessagePublisher(rabbitTemplate, objectMapper);
        }
    }

    @BeforeEach
    void setUp() {
        assertThat(messagePublisher).isNotNull();
    }

    @Test
    @DisplayName("1. 验证 RabbitMQ 连接健康检查")
    void testHealthCheck() {
        boolean healthy = messagePublisher.isHealthy();
        assertThat(healthy).isTrue();
    }

    @Test
    @DisplayName("2. 基本消息发布 - publish(Message)")
    void testBasicPublish() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello RabbitMQ - Basic Publish")
                .build();

        boolean result = messagePublisher.publish(message);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("3. 使用 MessageBuilder 构建并发布消息")
    void testPublishWithBuilder() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello RabbitMQ - Builder Pattern")
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
        // 使用默认 exchange（空字符串），因为 test.topic 不是一个有效的 exchange
        Message message = MessageBuilder.create()
                .textPayload("Hello RabbitMQ - With Routing Key")
                .build();

        boolean result = messagePublisher.publish(message, "", "test.key");
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("5. 带请求头发布 - publishWithHeaders")
    void testPublishWithHeaders() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello RabbitMQ - With Headers")
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
                .textPayload("Hello RabbitMQ - Async Publish")
                .build();

        messagePublisher.publishAsync(message);
    }

    @Test
    @DisplayName("7. 异步 Future 消息发布 - publishAsyncFuture")
    void testAsyncFuturePublish() throws Exception {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello RabbitMQ - Async Future")
                .build();

        Boolean result = messagePublisher.publishAsyncFuture(message).get(5, TimeUnit.SECONDS);
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
                .textPayload("Hello RabbitMQ - Transactional")
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
                .textPayload("Hello RabbitMQ - Specific Topic")
                .build();

        boolean result = messagePublisher.publish(message, TEST_TOPIC);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("12. MessageModel 发布")
    void testMessageModelPublish() {
        MessageModel<Void> messageModel = new MessageModel<>();
        messageModel.setTopic(TEST_TOPIC);
        messageModel.setPayload("Hello RabbitMQ - MessageModel");

        boolean result = messagePublisher.publish(messageModel);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("13. 获取发布器名称")
    void testGetPublisherName() {
        String publisherName = messagePublisher.getPublisherName();
        assertThat(publisherName).isEqualTo("RabbitMQMessagePublisher");
    }

    @Test
    @DisplayName("14. 发送同步确认消息 - sendSync")
    void testSendSync() {
        Message message = MessageBuilder.create(TEST_TOPIC)
                .textPayload("Hello RabbitMQ - Sync Message")
                .build();

        boolean result = messagePublisher.sendSync(message);
        assertThat(result).isTrue();
    }
}
