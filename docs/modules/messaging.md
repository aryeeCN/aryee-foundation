# Aryee Messaging 消息基础设施模块

> **所属项目**: [Aryee Foundation](../../README.md)
> **架构层次**: 平台层 (Platform Layer)
> **技术栈**: Java 21, Spring Boot 4.0.7, Kafka, RabbitMQ, RocketMQ, Redis, Reactor
> **消息模式**: 阻塞式 (Blocking) + 响应式 (Reactive)

## 简介

消息基础设施模块提供了统一的消息中间件抽象层，支持 Kafka、RabbitMQ、RocketMQ、Redis 四种消息后端，同时提供阻塞式和响应式两种编程模式，帮助开发者快速构建可靠的消息驱动系统。

该模块同时是事件驱动模块 (`aryee-foundation-event`) 的底层传输基础，事件模块通过 `EventUtil.toMessage()/fromMessage()` 将事件转换为消息后委托本模块投递。

### 核心特性

- **多中间件支持**: Kafka、RabbitMQ、RocketMQ、Redis 四种消息后端
- **双模式支持**: 阻塞式 (Blocking) 和响应式 (Reactive) 编程模式，独立 Starter 二选一
- **统一接口**: 统一的 `MessagePublisher` / `MessageConsumer` API，切换中间件无需修改业务代码
- **可靠性能力**: `MessageReliabilityService` 提供重试、死信队列、幂等去重
- **消息确认**: 支持手动确认 (`acknowledge`/`reject`) 与自动确认
- **顺序消息**: 支持 `publishOrdered` 按顺序键投递
- **事务消息**: 支持 `publishTransactional` / `commitTransaction` / `rollbackTransaction`
- **延迟消息**: 支持 `publishDelayed` 延迟投递
- **批量发送**: 支持单条、批量、Flux 流式发送

## 架构定位

- **依赖方向**: `Starter → Autoconfigure → Infrastructure → API`
- **API 层**: 纯接口与模型定义，不包含实现
- **Infrastructure 层**: 按阻塞/响应式 + 中间件类型分目录组织实现
- **跨模块协作**: 事件模块 (`event-infrastructure`) 依赖本模块的 `messaging-api`，通过 `MessagePublisher` 投递事件消息
- **版本管理**: 由 `aryee-foundation-bom` 统一管理，引用时无需声明 `<version>`

## 模块结构

```
aryee-foundation-messaging/
├── messaging-api                                  # 消息 API 抽象层（接口、模型、配置、注解）
├── messaging-infrastructure                       # 基础设施实现层
│   ├── blocking/
│   │   ├── kafka/                                 # Kafka 阻塞式实现
│   │   ├── rabbitmq/                              # RabbitMQ 阻塞式实现
│   │   ├── rocketmq/                              # RocketMQ 阻塞式实现
│   │   └── redis/                                 # Redis 阻塞式实现
│   └── reactive/
│       ├── kafka/                                 # Kafka 响应式实现
│       ├── rabbitmq/                              # RabbitMQ 响应式实现
│       └── rocketmq/                              # RocketMQ 响应式实现
├── messaging-spring-boot-autoconfigure            # 阻塞式 Spring Boot 自动配置
├── messaging-spring-boot-starter                  # 阻塞式 Starter（依赖聚合）
├── messaging-reactive-spring-boot-autoconfigure   # 响应式 Spring Boot 自动配置
└── messaging-reactive-spring-boot-starter         # 响应式 Starter（依赖聚合）
```

### 模块说明

| 模块 | 说明 |
|------|------|
| `messaging-api` | 核心 API 抽象，定义 `MessagePublisher`/`MessageConsumer`/`MessageReliabilityService` 等接口及 `Message`/`MessageModel` 模型 |
| `messaging-infrastructure` | 按 `blocking/{kafka,rabbitmq,rocketmq,redis}` 与 `reactive/{kafka,rabbitmq,rocketmq}` 分目录提供具体实现 |
| `messaging-spring-boot-autoconfigure` | 阻塞式自动配置，注册 `AryeeMessagingAutoConfiguration` |
| `messaging-spring-boot-starter` | 阻塞式 Starter，一键引入阻塞式全部依赖 |
| `messaging-reactive-spring-boot-autoconfigure` | 响应式自动配置，注册 `AryeeMessagingReactiveAutoConfiguration` |
| `messaging-reactive-spring-boot-starter` | 响应式 Starter，一键引入响应式全部依赖 |

> **隔离原则**: 阻塞式与响应式 Starter 互斥，禁止同时引入。阻塞实现中不出现 `Mono`/`Flux`，响应式实现中不出现阻塞调用。

## API 层包结构（messaging-api）

```
cn.aryee.messaging.api/
├── annotation/          # @MessageHandler、@MessageProducer、@MessageType 注解
├── config/              # MessagingProperties 配置属性类
├── extension/           # MessagePublisherExtension / MessageConsumerExtension 扩展点
├── model/               # Message、MessageModel、BaseMessageInfo、MessageBuilder、
│                        #   MessagePriority、MessageStatus、MessageType、DeadLetterMessage
├── service/             # MessagePublisher、MessageConsumer、MessagePublisherFactory、
│                        #   MessageReliabilityService 及对应 Reactive 版本
└── util/                # MessagePublisherLookup / MessagePublisherLoader（含 Reactive 版本）
```

## 使用方法

### Maven 依赖

#### 阻塞式 Starter

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>messaging-spring-boot-starter</artifactId>
</dependency>
```

#### 响应式 Starter

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>messaging-reactive-spring-boot-starter</artifactId>
</dependency>
```

> 版本由 `bom-full` 统一管理，无需声明 `<version>`。

### 配置选项

配置前缀：`aryee.messaging`

```yaml
aryee:
  messaging:
    # 是否启用消息组件
    enabled: true
    # 消息后端类型：kafka / rabbitmq / rocketmq / redis
    type: kafka
    # 消息服务器地址
    broker-url: tcp://localhost:61616
    # 客户端ID
    client-id: aryee-messaging
    # 是否自动启动连接
    auto-startup: true
    # 默认目的地名称
    default-destination:

    # 连接池配置
    pool:
      min-idle: 5
      max-active: 20
      max-wait: 60000
      initial-size: 5
      max-lifetime: 1800000
      idle-timeout: 600000
      test-interval: 30000

    # 重试配置
    retry:
      enabled: true
      max-attempts: 3
      initial-interval: 1000
      multiplier: 2.0
      max-interval: 30000
      jitter: true

    # 死信队列配置
    dead-letter:
      enabled: true
      queue-name: DLQ
      exchange-name: DLX
      routing-key: dead.letter
      retention-hours: 72

    # 消费者配置
    consumer:
      group-name: default-group
      concurrency: 1
      batch-size: 1
      batch-timeout: 1000
      consume-timeout: 30000
      manual-ack: false
      ack-mode: AUTO        # AUTO / MANUAL / BATCH
      poll-interval: 100
      max-poll-records: 500
      offset-reset: latest  # latest / earliest / none

    # 生产者配置
    producer:
      async: false
      compression-type: none   # none / gzip / snappy / lz4 / zstd
      send-timeout: 30000
      batch-size: 16384
      linger-ms: 0
      acks: all                # 0 / 1 / all
      transactional: false
      transaction-timeout: 60
      ordered: false
      max-message-bytes: 1048576

    # SQL日志配置
    sql-log:
      enabled: false
      show-params: true
      slow-query-threshold: 500
```

### 消息模型

`Message` 是核心消息模型，内部委托 `MessageModel<Void>` 承载字段，主要属性：

| 字段 | 类型 | 说明 |
|------|------|------|
| `messageId` | `String` | 消息ID |
| `topic` | `String` | 消息主题 |
| `tag` | `String` | 消息标签 |
| `payload` | `Object` | 消息内容 |
| `source` | `String` | 消息来源 |
| `target` | `String` | 目标服务 |
| `messageType` | `MessageType` | 消息类型（TEXT/JSON/XML/BINARY/OBJECT） |
| `priority` | `MessagePriority` | 优先级（LOW/NORMAL/HIGH/URGENT） |
| `status` | `MessageStatus` | 状态（CREATED/SENT/RECEIVED/ACKNOWLEDGED/CONSUMED/...） |
| `retryCount` / `maxRetryCount` | `Integer` | 重试计数 |
| `delayTime` | `Long` | 延迟时间（毫秒） |
| `expireTime` | `Date` | 过期时间 |
| `headers` | `Map<String, Object>` | 消息头 |

可通过 `MessageBuilder` 构建消息，或使用 `Message.fromMessageModel()` / `toMessageModel()` 与 `MessageModel` 互转。

### 消息发布

#### 阻塞式发布

```java
@Service
public class OrderMessageService {

    private final MessagePublisher messagePublisher;

    public OrderMessageService(MessagePublisher messagePublisher) {
        this.messagePublisher = messagePublisher;
    }

    /** 发布订单创建消息 */
    public void sendOrderCreated(Order order) {
        Message message = new Message();
        message.setMessageId(UUID.randomUUID().toString());
        message.setTopic("order-events");
        message.setTag("order.created");
        message.setPayload(order);
        message.setSource("order-service");
        message.setMessageType(MessageType.JSON);
        messagePublisher.publish(message);
    }

    /** 发布到指定主题并带路由键 */
    public void sendWithRouting(Order order) {
        Message message = buildMessage(order);
        messagePublisher.publish(message, "order-exchange", "order.created");
    }

    /** 延迟发布 */
    public void sendDelayed(Order order, long delayMs) {
        messagePublisher.publishDelayed(buildMessage(order), delayMs);
    }

    /** 顺序消息（按 orderKey 分区） */
    public void sendOrdered(Order order) {
        messagePublisher.publishOrdered(buildMessage(order), "order-status-exchange", order.getId().toString());
    }

    /** 事务消息 */
    public void sendTransactional(Order order) {
        messagePublisher.publishTransactional(buildMessage(order));
        // ... 本地事务 ...
        messagePublisher.commitTransaction();
        // 或 messagePublisher.rollbackTransaction();
    }

    /** 批量发布 */
    public void sendBatch(List<Order> orders) {
        List<Message> messages = orders.stream().map(this::buildMessage).toList();
        messagePublisher.publishBatch(messages);
    }

    private Message buildMessage(Order order) {
        Message message = new Message();
        message.setMessageId(UUID.randomUUID().toString());
        message.setTopic("order-events");
        message.setPayload(order);
        message.setMessageType(MessageType.JSON);
        return message;
    }
}
```

#### 响应式发布

```java
@Service
public class ReactiveOrderService {

    private final ReactiveMessagePublisher publisher;

    public ReactiveOrderService(ReactiveMessagePublisher publisher) {
        this.publisher = publisher;
    }

    /** 响应式发布单条 */
    public Mono<Boolean> send(Order order) {
        return publisher.publish(buildMessage(order));
    }

    /** 响应式批量发布，返回每条结果 */
    public Flux<Boolean> sendBatch(List<Order> orders) {
        List<Message> messages = orders.stream().map(this::buildMessage).toList();
        return publisher.publishBatchFlux(messages);
    }

    /** 响应式事务消息 */
    public Mono<Boolean> sendTransactional(Order order) {
        return publisher.publishTransactional(buildMessage(order))
                .flatMap(ok -> publisher.commitTransaction());
    }

    private Message buildMessage(Order order) {
        Message message = new Message();
        message.setMessageId(UUID.randomUUID().toString());
        message.setTopic("order-events");
        message.setPayload(order);
        message.setMessageType(MessageType.JSON);
        return message;
    }
}
```

### 消息消费

#### 阻塞式消费

```java
@Component
public class OrderMessageConsumer implements MessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderMessageConsumer.class);

    @Override
    public boolean consume(Message message) {
        try {
            Order order = (Order) message.getPayload();
            log.info("收到订单消息：messageId={}, topic={}", message.getMessageId(), message.getTopic());
            processOrder(order);
            return true;
        } catch (Exception e) {
            log.error("处理订单消息失败", e);
            return false;
        }
    }

    @Override
    public boolean consume(MessageModel<Void> messageModel) {
        return consume(Message.fromMessageModel(messageModel));
    }

    @Override
    public String[] supportedTopics() {
        return new String[]{"order-events"};
    }

    private void processOrder(Order order) {
        // 业务处理逻辑
    }
}
```

#### 带确认回调的消费

```java
boolean ok = messageConsumer.consumeWithAck(message, new MessageConsumer.AckCallback() {
    @Override
    public void onSuccess(Message message) {
        log.info("消息处理成功：{}", message.getMessageId());
    }

    @Override
    public void onFailure(Message message, Throwable error) {
        log.error("消息处理失败：{}", message.getMessageId(), error);
    }
});
```

### 可靠性服务

`MessageReliabilityService` 提供消息重试、死信队列、幂等去重能力，可供上层模块（如 Event）委托调用：

```java
@Service
public class ReliableMessageService {

    private final MessageReliabilityService reliabilityService;

    public ReliableMessageService(MessageReliabilityService reliabilityService) {
        this.reliabilityService = reliabilityService;
    }

    /** 带重试策略的发布（指数退避） */
    public void publishWithRetry(Message message) {
        reliabilityService.publishWithRetry(message, 3, 1000L);
    }

    /** 移入死信队列 */
    public void moveToDeadLetter(Message message, Throwable error) {
        reliabilityService.moveToDeadLetter(message, error);
    }

    /** 查询死信 */
    public List<DeadLetterMessage> listDeadLetters(String topic) {
        return reliabilityService.listDeadLetters(topic);
    }

    /** 重试死信消息 */
    public void retryDeadLetter(String messageId) {
        reliabilityService.retryDeadLetter(messageId);
    }

    /** 幂等性检查 */
    public boolean isDuplicate(String messageId) {
        return reliabilityService.isDuplicate(messageId);
    }
}
```

响应式场景使用 `ReactiveMessageReliabilityService`，方法返回 `Mono`/`Flux`。

## 中间件选型指南

| 中间件 | 适用场景 |
|--------|----------|
| **Kafka** | 高吞吐日志采集、事件溯源、实时数据流、消息回溯消费 |
| **RabbitMQ** | 灵活路由（Exchange 类型丰富）、订单通知、任务调度、延迟/死信队列 |
| **RocketMQ** | 分布式事务消息、严格顺序消息、金融级可靠性、电商交易 |
| **Redis** | 轻量级消息投递、跨服务广播、与 Redis 共存的基础设施 |

## 兼容性

- **Java**: 21+
- **Spring Boot**: 4.0.7
- **Reactor**: 与 Spring Boot 版本对齐
- **消息中间件**: Kafka 3.x+、RabbitMQ 3.11+、RocketMQ 5.x+、Redis 5.0+
- **Jakarta EE**: 9+（Servlet 6.0+）
