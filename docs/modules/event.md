# Aryee Event 事件驱动基础设施模块

> **所属项目**: [Aryee Foundation](../../README.md)
> **架构层次**: 平台层 (Platform Layer)
> **技术栈**: Java 21, Spring Boot 4.0.6, Reactor, Kafka, RabbitMQ, RocketMQ, Redis
> **事件模式**: 阻塞式 (Blocking) + 响应式 (Reactive)

## 简介

事件驱动基础设施模块提供了统一的事件发布订阅解决方案，支持进程内内存事件以及 Kafka、RabbitMQ、RocketMQ、Redis 四种分布式背板，同时提供阻塞式和响应式双模式。模块内置审计、安全、分发、可靠性四大增强服务，为分布式系统提供完整的事件驱动架构能力。

事件模块与消息基础设施模块 (`aryee-foundation-messaging`) 协同工作：分布式背板场景下，事件通过 `EventUtil.toMessage()` 转换为 `Message` 后委托 `MessagePublisher` 投递，接收端再通过 `EventUtil.fromMessage()` 还原为事件。

### 核心特性

- **多背板支持**: 内存（InMemory）+ Kafka / RabbitMQ / RocketMQ / Redis 四种分布式背板
- **双模式支持**: 阻塞式 (Blocking) 和响应式 (Reactive) 编程模型，独立 Starter 二选一
- **统一事件接口**: `EventBus` / `EventPublisher` / `EventListener` 统一抽象，切换背板无需修改业务代码
- **注解驱动**: `@EventPublish` 标注发布方法、`@EventListener` 标注监听方法（由 `EventAspect` 切面驱动）
- **审计服务**: `EventAuditService` 提供发布/消费/重试/投递状态记录与统计
- **安全服务**: `EventSecurityService` 提供签名验签、身份认证、访问控制、加解密、脱敏、防重放
- **分发服务**: `EventDistributionService` 提供路由、广播、扇出、优先级、限流、顺序保证
- **可靠性服务**: `EventReliabilityService` 提供投递保证、重试、死信队列、幂等去重、事件重放、积压处理
- **事件↔消息互转**: `EventUtil.toMessage()` / `fromMessage()` 实现事件与消息模型互转

## 架构定位

- **依赖方向**: `Starter → Autoconfigure → Infrastructure → API`
- **API 层**: 纯接口、模型、注解、配置定义，不含实现
- **Infrastructure 层**: 按 `blocking/` 与 `reactive/` 分目录，每个目录下按 `{back-end}` 与 `service/`、`aop/` 组织
- **跨模块协作**: 依赖 `messaging-api` 提供的 `Message` / `MessagePublisher` / `MessageReliabilityService`，将事件委托消息模块投递
- **版本管理**: 由 `aryee-foundation-bom` 统一管理，引用时无需声明 `<version>`

## 模块结构

```
aryee-foundation-event/
├── event-api                                  # 事件 API 抽象层（接口、模型、注解、配置、常量、异常）
├── event-infrastructure                       # 基础设施实现层
│   ├── blocking/
│   │   ├── aop/                               # EventAspect 注解切面
│   │   ├── kafka/                             # KafkaEventBus
│   │   ├── rabbitmq/                          # RabbitMqEventBus
│   │   ├── rocketmq/                          # RocketMqEventBus
│   │   ├── redis/                             # RedisEventBus + RedisEventAutoConfiguration
│   │   │   └── service/                       # RedisEventPublisher / Factory / Subscriber
│   │   └── service/                           # SimpleEventBus + DefaultEventPublisher + Factory
│   ├── reactive/
│   │   ├── kafka/                             # ReactiveKafkaEventBus
│   │   ├── rabbitmq/                          # ReactiveRabbitMqEventBus
│   │   ├── rocketmq/                          # ReactiveRocketMqEventBus
│   │   ├── redis/                             # ReactiveRedisEventAutoConfiguration
│   │   │   └── service/                       # ReactiveRedisEventPublisher / Factory / Subscriber
│   │   ├── service/                           # ReactiveSimpleEventBus + ReactiveDefaultEventPublisher + Factory
│   │   └── ReactiveEventAutoConfiguration.java
│   └── util/                                  # EventUtil（事件↔消息互转、事件ID生成、校验）
├── event-spring-boot-autoconfigure            # 阻塞式 Spring Boot 自动配置
├── event-spring-boot-starter                  # 阻塞式 Starter（依赖聚合）
├── event-reactive-spring-boot-autoconfigure   # 响应式 Spring Boot 自动配置
└── event-reactive-spring-boot-starter         # 响应式 Starter（依赖聚合）
```

### 模块说明

| 模块 | 说明 |
|------|------|
| `event-api` | 核心 API 抽象，定义 `EventBus`/`EventPublisher`/`EventListener` 及审计/安全/分发/可靠性服务接口与 `Event` 模型 |
| `event-infrastructure` | 按 `blocking/{kafka,rabbitmq,rocketmq,redis,service,aop}` 与 `reactive/{kafka,rabbitmq,rocketmq,redis,service}` 提供具体实现 |
| `event-spring-boot-autoconfigure` | 阻塞式自动配置，注册 `AryeeEventAutoConfiguration` |
| `event-spring-boot-starter` | 阻塞式 Starter，一键引入阻塞式全部依赖 |
| `event-reactive-spring-boot-autoconfigure` | 响应式自动配置，注册 `AryeeEventReactiveAutoConfiguration` |
| `event-reactive-spring-boot-starter` | 响应式 Starter，一键引入响应式全部依赖 |

> **隔离原则**: 阻塞式与响应式 Starter 互斥，禁止同时引入。

## API 层包结构（event-api）

```
cn.aryee.event.api/
├── annotation/          # @EventPublish、@EventListener 注解
├── config/              # EventProperties 配置属性类
├── constant/            # EventConstants 常量
├── exception/           # EventException 事件异常
├── model/               # Event、EventMetadata、EventAuditLog、EventRouteRule、
│                        #   EventSecurityContext、EventStatus、DeliveryGuarantee、
│                        #   DeadLetterEvent
├── service/             # EventBus、EventPublisher、EventPublisherFactory、EventListener、
│                        #   EventAuditService、EventSecurityService、
│                        #   EventDistributionService、EventReliabilityService
│                        #   及对应 Reactive 版本
├── stats/               # EventStatistics 事件统计
└── util/                # EventPublisherLookup / ReactiveEventPublisherLookup
```

## 核心服务接口

| 接口 | 职责 |
|------|------|
| `EventBus` | 事件总线：注册/注销监听器、同步/异步发布事件 |
| `EventPublisher` | 事件发布器：发布、异步发布、延迟发布、批量发布 |
| `EventListener` | 事件监听器：`onEvent` 处理事件、`supportedEventTypes` 声明支持类型 |
| `EventAuditService` | 审计：记录发布/消费成功/失败/重试/投递状态，查询审计日志，统计与分析死信 |
| `EventSecurityService` | 安全：签名验签、发布方认证、发布/消费鉴权、加解密、脱敏、防重放 Nonce |
| `EventDistributionService` | 分发：路由规则、广播、扇出、优先级投递、限流、顺序保证 |
| `EventReliabilityService` | 可靠性：投递保证（`DeliveryGuarantee`）、重试、死信队列、幂等去重、事件重放、积压处理 |

> `EventReliabilityService` 在分布式背板场景下委托 `messaging-api` 的 `MessageReliabilityService` 实现底层能力。

## 使用方法

### Maven 依赖

#### 阻塞式 Starter

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>event-spring-boot-starter</artifactId>
</dependency>
```

#### 响应式 Starter

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>event-reactive-spring-boot-starter</artifactId>
</dependency>
```

> 版本由 `bom-full` 统一管理，无需声明 `<version>`。

### 配置选项

配置前缀：`aryee.event`

```yaml
aryee:
  event:
    # 是否启用事件功能
    enabled: true
    # 事件总线实现类型：memory / kafka / rabbitmq / rocketmq / redis
    # 默认 memory（进程内），分布式场景使用 MQ 背板
    type: memory
    # 异步发布线程池大小
    async-pool-size: 4
    # 是否开启统计收集
    enable-statistics: false
    # 事件消费失败重试次数
    retry-count: 0
    # 重试间隔
    retry-delay: 1000
    # 重试间隔单位
    retry-time-unit: MILLISECONDS
    # 是否开启事件持久化
    enable-persistence: false
    # 分布式发布默认通道名
    default-channel: "aryee:event"
    # 默认是否异步发布
    default-async: false

    # 审计配置
    audit-enabled: false
    # 安全配置
    security-enabled: false
    sign-events: false
    # 可靠性配置
    reliability-enabled: false
    reliability-max-retries: 3
    # 分发配置
    distribution-enabled: false
```

### 事件模型

`Event` 是核心事件模型，主要属性：

| 字段 | 类型 | 说明 |
|------|------|------|
| `eventId` | `String` | 事件ID |
| `eventType` | `String` | 事件类型（如 `order.created`） |
| `source` | `String` | 事件源 |
| `data` | `Object` | 事件数据 |
| `timestamp` | `Date` | 事件时间 |
| `version` | `String` | 事件版本（默认 `1.0`） |
| `headers` | `Map<String, Object>` | 事件头信息 |

`EventUtil` 提供事件工具方法：`generateEventId()`、`validateEvent()`、`initializeEvent()`、`copyEvent()`，以及事件↔消息互转 `toMessage()` / `fromMessage()`。

### 内存事件使用

#### 发布事件

```java
@Service
public class OrderService {

    private final EventPublisher eventPublisher;

    public OrderService(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /** 同步发布事件 */
    public void publishOrderCreated(Order order) {
        Event event = new Event();
        event.setEventId(EventUtil.generateEventId());
        event.setEventType("order.created");
        event.setSource("order-service");
        event.setData(order);
        eventPublisher.publish(event);
    }

    /** 异步发布事件 */
    public void publishAsync(Order order) {
        Event event = buildEvent(order, "order.created");
        eventPublisher.publishAsync(event);
    }

    /** 延迟发布 */
    public void publishDelayed(Order order, long delayMs) {
        eventPublisher.publishDelayed(buildEvent(order, "order.created"), delayMs);
    }

    /** 批量发布 */
    public void publishBatch(List<Order> orders) {
        Event[] events = orders.stream()
                .map(o -> buildEvent(o, "order.created"))
                .toArray(Event[]::new);
        eventPublisher.publishBatch(events);
    }

    private Event buildEvent(Order order, String type) {
        Event event = new Event();
        event.setEventId(EventUtil.generateEventId());
        event.setEventType(type);
        event.setSource("order-service");
        event.setData(order);
        return event;
    }
}
```

#### 通过 EventBus 发布与订阅

```java
@Service
public class EventBusExample {

    private final EventBus eventBus;

    public EventBusExample(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void publish(Order order) {
        Event event = new Event();
        EventUtil.initializeEvent(event);
        event.setEventType("order.created");
        event.setSource("order-service");
        event.setData(order);
        eventBus.post(event);
    }
}

@Component
public class OrderEventListener implements EventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    @Override
    public void onEvent(Event event) {
        log.info("收到事件：type={}, eventId={}", event.getEventType(), event.getEventId());
        // 业务处理
    }

    @Override
    public String[] supportedEventTypes() {
        return new String[]{"order.created", "order.cancelled"};
    }

    @Override
    public boolean supports(String eventType) {
        for (String type : supportedEventTypes()) {
            if (type.equals(eventType)) {
                return true;
            }
        }
        return false;
    }
}
```

#### 注解驱动监听与发布

```java
@Component
public class OrderEventHandlers {

    private static final Logger log = LoggerFactory.getLogger(OrderEventHandlers.class);

    /** 注解监听事件 */
    @EventListener(value = {"order.created"}, async = true)
    public void onOrderCreated(Event event) {
        log.info("注解监听到订单创建事件：{}", event.getEventId());
    }

    @EventListener("order.cancelled")
    public void onOrderCancelled(Event event) {
        log.info("订单取消事件：{}", event.getEventId());
    }
}

@Service
public class OrderService {

    /** 注解发布事件：方法返回值或入参将被封装为事件 */
    @EventPublish(eventType = "order.created", source = "order-service", async = true)
    public Order createOrder(CreateOrderRequest request) {
        Order order = new Order();
        // 创建订单逻辑
        return order;
    }
}
```

### 分布式背板使用

切换到 Kafka/RabbitMQ/RocketMQ/Redis 背板时，事件会通过 `EventUtil.toMessage()` 转换为 `Message`，委托 `MessagePublisher` 投递到对应中间件，消费端通过 `EventUtil.fromMessage()` 还原事件后分发给本地监听器。

```yaml
aryee:
  event:
    type: redis           # 切换到 Redis 背板
    default-channel: "aryee:event"
    reliability-enabled: true
    reliability-max-retries: 3
```

```java
@Service
public class NotificationService {

    private final EventPublisher eventPublisher;

    public NotificationService(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /** 发布跨服务通知事件（经 Redis/MQ 背板广播） */
    public void sendNotification(String userId, String content) {
        Event event = new Event();
        event.setEventId(EventUtil.generateEventId());
        event.setEventType("notification.send");
        event.setSource("notification-service");
        event.setData(Map.of("userId", userId, "content", content));
        eventPublisher.publish(event);
    }
}
```

### 响应式事件使用

```java
@Service
public class ReactiveOrderService {

    private final ReactiveEventBus reactiveEventBus;
    private final ReactiveEventPublisher reactiveEventPublisher;

    public ReactiveOrderService(ReactiveEventBus reactiveEventBus,
                                ReactiveEventPublisher reactiveEventPublisher) {
        this.reactiveEventBus = reactiveEventBus;
        this.reactiveEventPublisher = reactiveEventPublisher;
    }

    /** 响应式发布 */
    public Mono<Void> createOrder(Order order) {
        Event event = buildEvent(order, "order.created");
        return reactiveEventPublisher.publish(event);
    }

    /** 通过响应式事件总线发布 */
    public Mono<Void> publishViaBus(Order order) {
        Event event = buildEvent(order, "order.created");
        return reactiveEventBus.post(event);
    }

    private Event buildEvent(Order order, String type) {
        Event event = new Event();
        event.setEventId(EventUtil.generateEventId());
        event.setEventType(type);
        event.setSource("order-service");
        event.setData(order);
        return event;
    }
}
```

### 增强服务使用

#### 可靠性服务

```java
@Service
public class ReliableEventService {

    private final EventReliabilityService reliabilityService;

    public ReliableEventService(EventReliabilityService reliabilityService) {
        this.reliabilityService = reliabilityService;
    }

    /** 以投递保证发布事件 */
    public void publishWithGuarantee(Event event) {
        reliabilityService.publishWithGuarantee(event, DeliveryGuarantee.EXACTLY_ONCE);
    }

    /** 以重试策略发布（指数退避） */
    public void publishWithRetry(Event event) {
        reliabilityService.publishWithRetry(event, 3, 1000L);
    }

    /** 查询并重放死信事件 */
    public void replayFailed(String eventId) {
        reliabilityService.replay(eventId);
    }

    /** 幂等性检查 */
    public boolean isDuplicate(String eventId) {
        return reliabilityService.isDuplicate(eventId);
    }

    /** 处理积压事件 */
    public void processBacklog(String eventType) {
        reliabilityService.processBacklog(eventType);
    }
}
```

#### 审计服务

```java
@Service
public class EventAuditQueryService {

    private final EventAuditService auditService;

    public EventAuditQueryService(EventAuditService auditService) {
        this.auditService = auditService;
    }

    /** 按条件查询审计日志 */
    public List<EventAuditLog> query(String eventType, String status, Date start, Date end) {
        return auditService.queryAuditLogs(null, eventType, status, start, end);
    }

    /** 按链路追踪ID查询 */
    public List<EventAuditLog> queryByTrace(String traceId) {
        return auditService.queryByTraceId(traceId);
    }

    /** 事件处理统计 */
    public Map<String, Object> statistics(String eventType, Date start, Date end) {
        return auditService.getProcessingStatistics(eventType, start, end);
    }
}
```

#### 安全服务

```java
@Service
public class EventSecurityFacade {

    private final EventSecurityService securityService;

    public EventSecurityFacade(EventSecurityService securityService) {
        this.securityService = securityService;
    }

    /** 签名与验签 */
    public String sign(Event event, String signKey) {
        return securityService.sign(event, signKey);
    }

    /** 发布/消费鉴权 */
    public boolean canPublish(String publisher, String eventType) {
        return securityService.canPublish(publisher, eventType);
    }

    /** 脱敏处理 */
    public Event desensitize(Event event) {
        return securityService.desensitize(event);
    }

    /** 防重放校验 */
    public boolean verifyNonce(String nonce, long timestamp) {
        return securityService.verifyNonce(nonce, timestamp);
    }
}
```

#### 分发服务

```java
@Service
public class EventDistributionFacade {

    private final EventDistributionService distributionService;

    public EventDistributionFacade(EventDistributionService distributionService) {
        this.distributionService = distributionService;
    }

    /** 广播事件 */
    public void broadcast(Event event) {
        distributionService.broadcast(event);
    }

    /** 扇出到多个目标 */
    public void fanOut(Event event, List<String> targets) {
        distributionService.fanOut(event, targets);
    }

    /** 优先级发布 */
    public void publishWithPriority(Event event, int priority) {
        distributionService.publishWithPriority(event, priority);
    }

    /** 限流 */
    public void rateLimit(String key, int permits, long durationMs) {
        distributionService.rateLimit(key, permits, durationMs);
    }
}
```

## 背板选型指南

| 背板 | 适用场景 |
|------|----------|
| **memory** | 单服务内模块解耦、性能要求高、事件不需跨服务、可接受事件丢失 |
| **redis** | 跨服务广播、轻量级分布式事件、与 Redis 共存的基础设施 |
| **kafka** | 高吞吐事件流、事件溯源、需要持久化与回溯消费 |
| **rabbitmq** | 灵活路由、订单通知、需要延迟/死信队列等高级特性 |
| **rocketmq** | 分布式事务事件、严格顺序事件、金融级可靠性 |

## 事件设计建议

- **命名规范**: 事件类型采用 `领域.动作`（过去式），如 `order.created`、`payment.completed`
- **载荷设计**: 事件 `data` 应包含足够业务信息，避免消费者额外查询
- **幂等处理**: 消费端配合 `EventReliabilityService.isDuplicate()` 做幂等控制
- **异常分类**: 业务异常记录后不重试；系统异常抛出触发重试机制

## 兼容性

- **Java**: 21+
- **Spring Boot**: 4.0.6
- **Reactor**: 与 Spring Boot 版本对齐
- **背板中间件**: Kafka 3.x+、RabbitMQ 3.11+、RocketMQ 5.x+、Redis 5.0+
- **Jakarta EE**: 9+（Servlet 6.0+）
- **依赖模块**: `aryee-foundation-messaging`（事件通过 `Message` 模型委托投递）
