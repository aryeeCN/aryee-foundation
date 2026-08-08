# Aryee Monitoring 监控基础设施模块

> **所属项目**: [Aryee Foundation](../../README.md)
> **技术栈**: Java 21、Spring Boot 4.0.6、Micrometer、OpenTelemetry、Apache SkyWalking、Zipkin Brave、Logback

## 简介

监控基础设施模块提供统一的应用可观测性解决方案，覆盖指标收集、链路追踪、日志聚合、告警通知、审计与限流等核心能力。模块基于 **Micrometer + OpenTelemetry + SkyWalking** 三大体系构建，采用 Blocking / Reactive 双模式设计，屏蔽底层实现差异，为分布式系统提供标准化监控契约。

### 核心特性

- ✅ **指标收集（Metric）**: 基于 Micrometer 的 `MetricService`，支持单条 / 批量记录与按标签查询
- ✅ **链路追踪（Tracing）**: 统一 `TracingService` 契约，支持 OpenTelemetry / Zipkin / Jaeger / SkyWalking / NoOp 多提供商
- ✅ **日志聚合（Logging）**: 基于 Logback 的 `LoggingService`，支持按时间范围 / 标签查询
- ✅ **告警通知（Alert）**: 多级别告警（INFO / WARNING / ERROR / CRITICAL）生命周期管理
- ✅ **多通知渠道 SPI**: 内置邮件、Webhook、钉钉、飞书、短信五种 `AlertChannel` 实现，支持按级别路由
- ✅ **审计能力（Audit）**: `AuditService` + 合规校验（`ComplianceCriteria` / `ComplianceCheckResult`）
- ✅ **注解驱动埋点**: `@AlertMonitor` / `@MetricCollect` / `@LogOperation` / `@PerformanceMonitor` / `@RateLimit` 等注解
- ✅ **双模式隔离**: Blocking / Reactive 严格分层，独立 Starter，禁止同时引入
- ✅ **统一异常体系**: `RateLimiterUtil` 采用 `SystemException` 替代 `RuntimeException`，与全局异常体系保持一致

## 架构定位

- **依赖方向**: `Starter → Autoconfigure → Infrastructure → API`
- **公共基础**: 依赖 `commons-core` / `commons-domain` / `commons-spring`
- **版本管理**: 由 `aryee-foundation-bom` 统一管理，子模块依赖禁止声明 `<version>`
- **双模式隔离**: Blocking 与 Reactive 使用独立的 Starter / Autoconfigure 模块，用户二选一引入

## 模块结构

```
aryee-foundation-monitoring/                              # 聚合 POM (artifactId=aryee-foundation-monitoring)
├── monitoring-api/                                       # API 契约层
│   └── cn.aryee.monitoring.api/
│       ├── alert/channel/                                # 告警渠道契约
│       │   ├── AlertChannel.java                         # 渠道统一接口（getName/send/supports）
│       │   ├── AlertChannelProperties.java               # 渠道配置 (prefix=aryee.monitoring.alert.channel)
│       │   └── AlertChannelRouter.java                   # 渠道路由契约
│       ├── annotation/                                   # 监控注解
│       │   ├── AlertMonitor / MetricCollect / MetricMeta
│       │   ├── LogExecutionTime / LogOperation / LogRecordMeta
│       │   ├── PerformanceMonitor
│       │   └── RateLimit / RateLimiterMeta
│       ├── enums/                                        # 枚举
│       │   ├── AlertLevel / MetricType
│       │   ├── AuditLevelEnum / AuditStatusEnum / BuzLogModelEnum
│       │   ├── OperationLogType / RateLimiterMode
│       ├── exception/                                    # 异常
│       │   ├── MonitoringException
│       │   ├── AlertNotFoundException
│       │   └── MetricNotFoundException
│       ├── model/                                        # 数据模型
│       │   ├── Alert / AlertStatus
│       │   ├── Metric / LogInfo / Notification
│       │   └── audit/                                    # 审计子模型
│       │       ├── SimpleAuditRecord / AuditQueryCriteria / AuditStatistics
│       │       └── ComplianceCriteria / ComplianceCheckResult
│       ├── service/                                      # 服务契约（Blocking + Reactive + Factory）
│       │   ├── AlertService / ReactiveAlertService (+ Factory)
│       │   ├── MetricService / ReactiveMetricService (+ Factory)
│       │   ├── LoggingService / ReactiveLoggingService (+ Factory)
│       │   ├── MetricsExporterService (+ Factory)
│       │   ├── AuditService
│       │   └── ReactiveNotificationCenter
│       ├── support/Stopwatch.java                        # 计时工具
│       ├── tracing/                                      # 链路追踪契约
│       │   ├── TracingService / ReactiveTracingService
│       │   ├── TracingProvider                           # 枚举：SKYWALKING / ZIPKIN / JAEGER / NONE
│       │   ├── TracingServiceFactory
│       │   ├── model/Span / model/TraceContext
│       │   ├── support/TraceLogger
│       │   └── util/TraceTransmitUtil
│       └── util/                                         # 工具类
│           ├── JvmMonitorUtil / MonitoringUtil
│           ├── PerformanceProfilingUtil
│           └── RateLimiterUtil
├── monitoring-infrastructure/                            # 实现层（Blocking + Reactive 双实现）
│   └── cn.aryee.monitoring.infrastructure/
│       ├── alert/channel/                                # 告警渠道实现（SPI）
│       │   ├── AbstractAlertChannel                      # 抽象基类
│       │   ├── AlertChannelRouterImpl                    # 路由实现
│       │   ├── EmailAlertChannel                         # 邮件
│       │   ├── WebhookAlertChannel                       # Webhook
│       │   ├── DingTalkAlertChannel                      # 钉钉
│       │   ├── FeishuAlertChannel                        # 飞书
│       │   ├── SmsAlertChannel / NoopSmsAlertChannel     # 短信（默认 Noop）
│       ├── blocking/                                     # Blocking 实现
│       │   ├── inmemory/                                 # InMemoryAlertService + Factory
│       │   ├── logback/                                  # LogbackLoggingService + Factory
│       │   ├── micrometer/                               # MicrometerMetricService + Factory
│       │   └── tracing/                                  # Tracing 实现
│       │       ├── autoconfigure/TracingProperties       # 追踪配置 (prefix=aryee.monitoring.tracing)
│       │       ├── AbstractTracingService / NoOpTracingService
│       │       ├── OpenTelemetryTracingService
│       │       ├── jaeger/JaegerTracingService
│       │       ├── skywalking/SkyWalkingTracingService
│       │       └── zipkin/ZipkinTracingService
│       └── reactive/                                     # Reactive 实现
│           ├── inmemory/                                 # ReactiveInMemoryAlertService + Factory
│           ├── logback/                                  # ReactiveLogbackLoggingService + Factory
│           └── micrometer/                               # ReactiveMicrometerMetricService + Factory
├── monitoring-spring-boot-autoconfigure/                 # Blocking 自动配置
│   └── 注册: AryeeMonitoringAutoConfiguration / TracingAutoConfiguration / AlertChannelAutoConfiguration
├── monitoring-spring-boot-starter/                       # Blocking Starter 依赖聚合
├── monitoring-reactive-spring-boot-autoconfigure/        # Reactive 自动配置
│   └── AryeeMonitoringReactiveAutoConfiguration / MonitoringReactiveProperties
└── monitoring-reactive-spring-boot-starter/              # Reactive Starter 依赖聚合
```

### 模块说明

| 模块 | artifactId | 说明 |
|------|-----------|------|
| monitoring-api | `monitoring-api` | Blocking + Reactive 契约接口、模型、注解、配置属性 |
| monitoring-infrastructure | `monitoring-infrastructure` | Blocking + Reactive 双实现（Micrometer / OpenTelemetry / Logback / 渠道 SPI） |
| monitoring-spring-boot-autoconfigure | `monitoring-spring-boot-autoconfigure` | Blocking 自动配置（3 个 AutoConfiguration） |
| monitoring-spring-boot-starter | `monitoring-spring-boot-starter` | Blocking Starter，开箱即用 |
| monitoring-reactive-spring-boot-autoconfigure | `monitoring-reactive-spring-boot-autoconfigure` | Reactive 自动配置（`AryeeMonitoringReactiveAutoConfiguration`） |
| monitoring-reactive-spring-boot-starter | `monitoring-reactive-spring-boot-starter` | Reactive Starter，全栈响应式 |

## 使用方法

### Maven 依赖

#### Blocking 模式（默认，Servlet / WebMVC 场景）

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>monitoring-spring-boot-starter</artifactId>
</dependency>
```

#### Reactive 模式（WebFlux 场景）

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>monitoring-reactive-spring-boot-starter</artifactId>
</dependency>
```

> - 版本由 BOM 统一管理，无需声明 `<version>`。
> - Blocking 与 Reactive Starter 禁止同时引入。
> - 可选第三方依赖（Micrometer / OpenTelemetry / SkyWalking / Zipkin / Mail / AOP）按需引入，自动配置通过 `@ConditionalOnClass` 装配。

### 配置选项

模块配置前缀为 `aryee.monitoring`，包含追踪、告警渠道、响应式三个子配置。

```yaml
aryee:
  monitoring:
    # 链路追踪配置（TracingProperties, prefix=aryee.monitoring.tracing）
    tracing:
      enabled: true
      provider: zipkin          # SKYWALKING / ZIPKIN / JAEGER / NONE
      service-name: aryee-foundation
      sampling-rate: 1.0        # 采样率 0.0~1.0
      mdc-enabled: true         # 是否写入 MDC
      # 请求入口 Trace 过滤器（自动初始化 commons TraceContext + 响应头回传 traceId）
      trace-filter:
        enabled: true
        trace-id-header: traceparent           # W3C TraceContext 主请求头
        fallback-trace-id-headers: [X-B3-TraceId, x-trace-id]  # 兼容其他追踪体系
        fallback-span-id-headers: [X-B3-SpanId]
        response-trace-id-header: x-trace-id   # 写入响应头便于前端排查
        exclude-patterns:                       # 跳过的路径（Ant 风格）
          - /actuator/**
          - /error
          - "*.html"
      skywalking:
        collector-url: http://localhost:11800
        protocol: grpc
        agent-namespace: ""
        agent-name: ""
      zipkin:
        base-url: http://localhost:9411
        encoder: json
        sender-type: web
      jaeger:
        endpoint: http://localhost:14268/api/traces
        sampler-type: const
        sampler-param: 1.0
        agent-host: localhost
        agent-port: 6831

    # 告警通知渠道配置（AlertChannelProperties, prefix=aryee.monitoring.alert.channel）
    alert:
      channel:
        enabled: false          # 渠道总开关
        email:
          enabled: false
          to: []                # 收件人列表
          from:                 # 为空时使用 spring.mail.username
          subject-prefix: "[Aryee Alert]"
        webhook:
          enabled: false
          url:                  # Webhook 接收地址
          timeout: 5000
          headers: {}           # 自定义请求头
        dingtalk:
          enabled: false
          webhook:              # 钉钉机器人地址
          secret:               # 加签密钥
          markdown: true
        feishu:
          enabled: false
          webhook:              # 飞书机器人地址
          card: true
        sms:
          enabled: false
          phones: []            # 手机号列表
          sign-name:            # 短信签名
          template-code:        # 短信模板 ID
        routing:                # 按告警级别路由到渠道
          INFO: [email]
          WARNING: [email, dingtalk]
          ERROR: [email, dingtalk, feishu]
          CRITICAL: [email, dingtalk, feishu, sms]

    # 响应式监控配置（MonitoringReactiveProperties, prefix=aryee.monitoring.reactive）
    reactive:
      enabled: true
      metrics:
        enabled: true
      alerts:
        enabled: true
      logs:
        enabled: true
      # 响应式链路追踪入口过滤器（WebFlux）
      tracing:
        enabled: true
        service-name: aryee-foundation
        trace-id-header: traceparent
        fallback-trace-id-headers: [X-B3-TraceId, x-trace-id]
        fallback-span-id-headers: [X-B3-SpanId]
        response-trace-id-header: x-trace-id
        exclude-patterns:
          - /actuator/**
          - /error
          - "*.html"
```

### 指标收集示例

```java
@Service
public class OrderMetricService {

    private final MetricService metricService;

    public OrderMetricService(MetricService metricService) {
        this.metricService = metricService;
    }

    /** 记录单条指标 */
    public void record(String name, double value, Map<String, String> tags) {
        Metric metric = new Metric();
        metric.setName(name);
        metric.setValue(value);
        metric.setTags(tags);
        metricService.recordMetric(metric);
    }

    /** 批量记录 */
    public void recordBatch(List<Metric> metrics) {
        metricService.recordMetrics(metrics);
    }

    /** 按标签查询指标 */
    public List<Metric> queryByTags(Map<String, String> tags) {
        return metricService.getMetricsByTags(tags);
    }
}
```

### 告警管理示例

```java
@Service
public class AlertExampleService {

    private final AlertService alertService;

    public AlertExampleService(AlertService alertService) {
        this.alertService = alertService;
    }

    /** 创建告警 */
    public String createAlert(Alert alert) {
        return alertService.createAlert(alert);
    }

    /** 触发告警（将路由到配置的渠道） */
    public void trigger(String alertId) {
        alertService.triggerAlert(alertId);
    }

    /** 告警生命周期流转 */
    public void acknowledge(String alertId) { alertService.acknowledgeAlert(alertId); }
    public void resolve(String alertId)     { alertService.resolveAlert(alertId); }
    public void close(String alertId)       { alertService.closeAlert(alertId); }

    /** 按状态查询告警 */
    public List<Alert> getByStatus(AlertStatus status) {
        return alertService.getAlertsByStatus(status);
    }
}
```

### 链路追踪示例

```java
@Service
public class TracingExampleService {

    private final TracingService tracingService;

    public TracingExampleService(TracingService tracingService) {
        this.tracingService = tracingService;
    }

    /** 手动开启 Trace 与 Span */
    public void doTracedWork(String operation) {
        TraceContext ctx = tracingService.startTrace("order-service", operation);
        Span span = tracingService.startSpan(operation, ctx);
        try {
            tracingService.setAttribute(span, "biz.key", "value");
            tracingService.addEvent(span, "process.start");
            // 业务逻辑
            tracingService.addEvent(span, "process.end");
        } catch (Exception e) {
            tracingService.recordException(span, e);
            throw e;
        } finally {
            tracingService.endSpan(span);
        }
    }

    /** 获取当前 TraceId */
    public String currentTraceId() {
        return tracingService.getTraceId();
    }

    /** 上下文传播 */
    public void propagate(Object carrier) {
        TraceContext ctx = tracingService.getCurrentTraceContext();
        tracingService.inject(ctx, carrier);
    }
}
```

### R 响应自动带 traceId 示例

引入 monitoring starter 后，所有经过 Spring 控制器返回的 `R<T>` 响应都会自动附带当前请求链路的 `traceId`，无需手动调用 `.withTraceId()`：

```java
@RestController
public class OrderController {

    @GetMapping("/orders/{id}")
    public R<OrderVO> getOrder(@PathVariable Long id) {
        // 不需要手动 .withTraceId()，restResult 内部会自动从 TraceContext / Micrometer Tracing 获取
        return R.ok(orderService.findById(id))
                .addExtra("total", 1);
    }
}
```

输出示例（注意 `extra.traceId`）：

```json
{
  "code": 200,
  "msg": "success",
  "data": { "id": 1, "name": "xxx" },
  "timestamp": 1714960000000,
  "extra": {
    "total": 1,
    "traceId": "4bf92f3577b34da6a3ce929d0e0e4736"
  }
}
```

> **实现原理**：TraceFilter（Blocking）/ TraceWebFilter（Reactive）作为请求入口初始化 `commons` 的 `TraceContext`，
> 而 `R.restResult()` 在构造响应时会调用 `TraceContext.getTraceId()` 自动写入 extra；当项目接入 Micrometer Tracing + OTel
> 时，监控模块通过 `MicrometerTraceIdSupplier` SPI 把真实的 OTel traceId 回写到 `TraceContext.getTraceId()`。

### 与 Micrometer Observation / OpenTelemetry 集成（推荐 Spring Boot 4.x）

只需在业务项目中引入如下依赖即可自动联动：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<!-- Micrometer Tracing (桥接 OpenTelemetry) -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

TracingAutoConfiguration 自动通过 `@ConditionalOnBean(type = "io.micrometer.tracing.Tracer")` 把 Spring 容器中的 `Tracer` 回写到 `MicrometerTraceIdSupplier`，从而：

1. 所有 `R<T>` 返回值自动带 OTel 的真实 traceId
2. `TraceContext.getTraceId()` 不再是随机 UUID，而是 OTel 全局 traceId
3. 配合 `management.tracing.sampling.probability=1.0` 实现全链路采样

### 日志聚合示例

```java
@Service
public class LoggingExampleService {

    private final LoggingService loggingService;

    public LoggingExampleService(LoggingService loggingService) {
        this.loggingService = loggingService;
    }

    /** 记录日志 */
    public void log(String message, String level, Map<String, String> tags) {
        loggingService.log(message, level, tags);
    }

    /** 按时间范围查询 */
    public List<Map<String, Object>> queryByTime(long start, long end) {
        return loggingService.queryLogsByTimeRange(start, end);
    }

    /** 按标签查询 */
    public List<Map<String, Object>> queryByTags(Map<String, String> tags) {
        return loggingService.queryLogsByTags(tags);
    }
}
```

## 兼容性

### 运行环境

| 项 | 版本 |
|----|------|
| Java | 21+ |
| Spring Boot | 4.0.6 |
| Micrometer | 跟随 Spring Boot 版本 |
| OpenTelemetry | 1.x |
| Apache SkyWalking | apm-toolkit-trace |

### 链路追踪提供商

| 提供商 | 实现类 | 默认 |
|--------|--------|------|
| OpenTelemetry | `OpenTelemetryTracingService` | — |
| Zipkin | `ZipkinTracingService` | ✅（`TracingProvider.ZIPKIN`） |
| Jaeger | `JaegerTracingService` | — |
| SkyWalking | `SkyWalkingTracingService` | — |
| None | `NoOpTracingService` | — |

### 告警通知渠道

| 渠道 | 实现类 | 配置项 |
|------|--------|--------|
| 邮件 | `EmailAlertChannel` | `aryee.monitoring.alert.channel.email.*` |
| Webhook | `WebhookAlertChannel` | `aryee.monitoring.alert.channel.webhook.*` |
| 钉钉 | `DingTalkAlertChannel` | `aryee.monitoring.alert.channel.dingtalk.*` |
| 飞书 | `FeishuAlertChannel` | `aryee.monitoring.alert.channel.feishu.*` |
| 短信 | `SmsAlertChannel` / `NoopSmsAlertChannel` | `aryee.monitoring.alert.channel.sms.*` |

### 服务实现矩阵

| 服务契约 | Blocking 实现 | Reactive 实现 |
|---------|--------------|--------------|
| `AlertService` | `InMemoryAlertService` | `ReactiveInMemoryAlertService` |
| `MetricService` | `MicrometerMetricService` | `ReactiveMicrometerMetricService` |
| `LoggingService` | `LogbackLoggingService` | `ReactiveLogbackLoggingService` |
