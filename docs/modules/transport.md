# Aryee Transport 统一传输层模块

> **所属项目**: [Aryee Foundation](../../README.md)
> **架构层次**: 基础设施层 (Foundation Layer)
> **技术栈**: Java 21, Spring Boot 4.0.6, Spring Cloud OpenFeign, Spring WebFlux WebClient, Spring Cloud LoadBalancer, Nacos Discovery, Resilience4j
> **传输模式**: 阻塞式 (Blocking), 响应式 (Reactive)

## 简介

统一传输层模块，提供入站 HTTP（WebMVC / WebFlux）和出站 HTTP（OpenFeign / WebClient）的统一抽象。通过统一的 `TransportService` / `ReactiveTransportService` 接口，实现入站请求拦截和出站服务调用的统一管理。

### 核心特性

- ✅ **双模式支持**: 阻塞式（WebMVC + OpenFeign）和响应式（WebFlux + WebClient）两种编程模型
- ✅ **入站统一抽象**: 统一的 `InboundFilter` SPI，支持请求日志、CORS、限流、全局异常处理等横切关注点
- ✅ **出站统一抽象**: 统一的 `OutboundInterceptor` SPI，支持重试、熔断、超时等弹性能力（基于 Resilience4j）
- ✅ **出站熔断保护**: 基于 Resilience4j 的按下游服务维度熔断，零改造装饰器自动包装（Blocking / Reactive 双模式）
- ✅ **统一请求/响应模型**: `TransportRequest` / `TransportResponse` 贯穿入站和出站全链路
- ✅ **声明式端点**: `@InboundEndpoint` 注解标记入站端点，`@OutboundClient` / `@OutboundMethod` 注解声明出站服务
- ✅ **服务配置管理**: 运行时动态注册/查询出站服务配置（`OutboundServiceConfig`）
- ✅ **Nacos 服务发现自动适配**: 微服务工程引入 `spring-cloud-starter-alibaba-nacos-discovery` 后自动生效（基础能力库不引入 Nacos 依赖，通过 `@ConditionalOnClass` 字符串守卫）

## 架构定位

```
aryee-foundation-transport/                       ← 聚合 POM（packaging=pom）
├── transport-api/                                ← 契约层
│   ├── annotation/     @InboundEndpoint, @OutboundClient, @OutboundMethod
│   ├── config/         TransportProperties（纯 POJO，非 @ConfigurationProperties）
│   ├── constant/       HttpConstants
│   ├── enums/          TransportMode (INBOUND/OUTBOUND), OutboundClientType (FEIGN/RESTTEMPLATE/WEBCLIENT)
│   ├── exception/      TransportException, InboundException, OutboundTimeoutException
│   ├── model/          TransportRequest, TransportResponse, OutboundServiceConfig
│   └── service/        TransportService, ReactiveTransportService, InboundFilter, OutboundInterceptor
│
├── transport-infrastructure/                     ← 实现层
│   ├── common/
│   │   ├── exception/InboundExceptionAdvice     ← @RestControllerAdvice 全局异常处理
│   │   ├── service/DefaultTransportService     ← Blocking 默认实现
│   │   ├── service/DefaultReactiveTransportService ← Reactive 默认实现
│   │   └── util/TransportUtil
│   ├── inbound/blocking/                         ← WebMVC 入站
│   │   ├── config/BlockingInboundConfig         ← CORS 配置
│   │   ├── filter/BlockingInboundFilterChain   ← Servlet 过滤器链
│   │   └── interceptor/WebMvcRequestLogInterceptor ← 请求日志拦截器
│   ├── inbound/reactive/                         ← WebFlux 入站
│   │   ├── filter/ReactiveInboundFilterChain    ← WebFlux 过滤器链
│   │   └── webfilter/WebFluxRequestLogFilter    ← 请求日志 WebFilter
│   ├── outbound/blocking/                        ← OpenFeign 出站
│   │   ├── client/BlockingOutboundClientFactory
│   │   ├── feign/FeignOutboundClient
│   │   ├── feign/discovery/FeignNacosLoadBalancerAutoConfiguration ← Nacos LoadBalancer 适配
│   │   └── service/BlockingOutboundService
│   └── outbound/reactive/                        ← WebClient 出站
│       ├── client/ReactiveOutboundClientFactory
│       ├── webclient/WebClientOutboundClient
│       ├── discovery/ReactiveNacosLoadBalancerAutoConfiguration ← Nacos LoadBalancer 适配
│       └── service/ReactiveOutboundService
│
├── transport-spring-boot-autoconfigure/          ← Blocking 自动装配
│   ├── AryeeTransportAutoConfiguration          （@ConditionalOnClass: DispatcherServlet + feign.Client）
│   └── TransportProps                           （@ConfigurationProperties prefix = "aryee.transport"）
│
├── transport-reactive-spring-boot-autoconfigure/ ← Reactive 自动装配
│   ├── AryeeTransportReactiveAutoConfiguration  （@ConditionalOnClass: DispatcherHandler + WebClient）
│   └── TransportReactiveProps                    （@ConfigurationProperties prefix = "aryee.transport.reactive"）
│
├── transport-spring-boot-starter/               ← Blocking Starter 依赖聚合（WebMVC + OpenFeign）
└── transport-reactive-spring-boot-starter/      ← Reactive Starter 依赖聚合（WebFlux + WebClient）
```

- **依赖方向**: Starter → Autoconfigure → Infrastructure → API
- **依赖 commons**: `commons-core` / `commons-domain` / `commons-spring` / `commons-web`
- **双模式隔离**（参见 architecture.md §6.1）：Blocking 与 Reactive 使用独立的 Starter / Autoconfigure 模块，用户二选一引入，禁止同时引入

## Maven 依赖

### Blocking 模式（WebMVC + OpenFeign）

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>transport-spring-boot-starter</artifactId>
    <!-- 版本由 bom-full 统一管理，无需指定 -->
</dependency>
```

Starter 已聚合的 `optional` 依赖（按需传递给使用方）：
- `spring-boot-starter-web`
- `spring-cloud-starter-openfeign`
- `spring-cloud-starter-loadbalancer`
- `resilience4j-circuitbreaker` / `resilience4j-retry`
- `spring-boot-starter-validation`

### Reactive 模式（WebFlux + WebClient）

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>transport-reactive-spring-boot-starter</artifactId>
</dependency>
```

Starter 已聚合的 `optional` 依赖：
- `spring-boot-starter-webflux`
- `spring-cloud-starter-loadbalancer`
- `resilience4j-circuitbreaker` / `resilience4j-retry`
- `spring-boot-starter-validation`

## 配置说明

### Blocking 配置前缀：`aryee.transport`（`TransportProps`）

```yaml
aryee:
  transport:
    # 是否启用传输模块，默认 true
    enabled: true
    # 是否启用 CORS 跨域支持，默认 true
    cors-enabled: true
    # 是否启用请求日志，默认 true
    request-log-enabled: true
    # 默认出站客户端类型：FEIGN / RESTTEMPLATE
    default-client-type: FEIGN
    # 默认超时时间（毫秒）
    default-timeout-ms: 10000
    outbound:
      circuit-breaker:
        # 是否启用出站熔断，默认 false
        enabled: true
        # 失败率阈值（百分比），默认 50
        failure-rate-threshold: 50
        # 熔断打开后等待时长（毫秒），默认 30000
        wait-duration-in-open-state-ms: 30000
        # 半开状态允许调用次数，默认 10
        permitted-calls-in-half-open-state: 10
        # 滑动窗口大小，默认 100
        sliding-window-size: 100
        # 触发失败率计算的最小调用次数，默认 10
        minimum-number-of-calls: 10
```

### Reactive 配置前缀：`aryee.transport.reactive`（`TransportReactiveProps`）

```yaml
aryee:
  transport:
    reactive:
      enabled: true
      cors-enabled: true
      request-log-enabled: true
      # 默认出站客户端类型：WEBCLIENT
      default-client-type: WEBCLIENT
      default-timeout-ms: 10000
```

> 响应式模式下熔断配置与阻塞式共用同一键空间 `aryee.transport.outbound.circuit-breaker.*`（由 `TransportOutboundProps` 绑定）。

> `TransportProperties`（位于 `transport-api`）是聚合入站 / 出站 / 响应式 / 服务发现 / CORS / 限流 / 熔断器的全量 POJO 模型，可作为代码内 API 使用；实际 Spring Boot `@ConfigurationProperties` 由 `TransportProps`（Blocking）与 `TransportReactiveProps`（Reactive）分别承担。

## 使用示例

### 1. Blocking 出站调用（WebMVC + OpenFeign）

```java
import cn.aryee.transport.api.enums.OutboundClientType;
import cn.aryee.transport.api.enums.TransportMode;
import cn.aryee.transport.api.model.OutboundServiceConfig;
import cn.aryee.transport.api.model.TransportRequest;
import cn.aryee.transport.api.model.TransportResponse;
import cn.aryee.transport.api.service.TransportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    @Autowired
    private TransportService transportService;

    public TransportResponse callUserService(Long userId) {
        TransportRequest request = TransportRequest.builder()
                .mode(TransportMode.OUTBOUND)
                .method("GET")
                .path("/api/users/" + userId)
                .addHeader("Accept", "application/json")
                .timeoutMs(5000L)
                .build();

        return transportService.sendWithRetry(request, 3);
    }

    public void registerUserService() {
        OutboundServiceConfig config = OutboundServiceConfig.builder()
                .serviceName("user-service")
                .baseUrl("http://user-service:8080")
                .clientType(OutboundClientType.FEIGN)
                .retryPolicy(OutboundServiceConfig.RetryPolicy.builder()
                        .maxRetries(3)
                        .initialDelayMs(100)
                        .backoffMultiplier(2.0)
                        .maxDelayMs(5000)
                        .build())
                .build();

        transportService.registerServiceConfig(config);
    }
}
```

### 2. Reactive 出站调用（WebFlux + WebClient）

```java
import cn.aryee.transport.api.enums.TransportMode;
import cn.aryee.transport.api.model.TransportRequest;
import cn.aryee.transport.api.model.TransportResponse;
import cn.aryee.transport.api.service.ReactiveTransportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ReactiveOrderService {

    @Autowired
    private ReactiveTransportService reactiveTransportService;

    public Mono<TransportResponse> callUserService(Long userId) {
        TransportRequest request = TransportRequest.builder()
                .mode(TransportMode.OUTBOUND)
                .method("GET")
                .path("/api/users/" + userId)
                .addHeader("Accept", "application/json")
                .timeoutMs(5000L)
                .build();

        return reactiveTransportService.sendWithRetry(request, 3);
    }

    public Mono<TransportResponse> callWithTimeout(Long userId) {
        TransportRequest request = TransportRequest.builder()
                .mode(TransportMode.OUTBOUND)
                .method("GET")
                .path("/api/users/" + userId)
                .build();

        return reactiveTransportService.sendWithTimeout(request, 3000L);
    }
}
```

### 3. 自定义入站过滤器（实现 `InboundFilter` SPI）

```java
import cn.aryee.transport.api.model.TransportRequest;
import cn.aryee.transport.api.model.TransportResponse;
import cn.aryee.transport.api.service.InboundFilter;
import org.springframework.stereotype.Component;

@Component
public class AuthInboundFilter implements InboundFilter {

    @Override
    public boolean preHandle(TransportRequest request) {
        String token = request.getHeader("Authorization");
        return token != null && !token.isEmpty();
    }

    @Override
    public void postHandle(TransportRequest request, TransportResponse response) {
        // 后置处理
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
```

### 4. 自定义出站拦截器（实现 `OutboundInterceptor` SPI）

```java
import cn.aryee.transport.api.model.OutboundServiceConfig;
import cn.aryee.transport.api.model.TransportRequest;
import cn.aryee.transport.api.model.TransportResponse;
import cn.aryee.transport.api.service.OutboundInterceptor;
import cn.aryee.transport.infrastructure.common.util.TransportUtil;
import org.springframework.stereotype.Component;

@Component
public class TraceOutboundInterceptor implements OutboundInterceptor {

    @Override
    public boolean beforeRequest(TransportRequest request, OutboundServiceConfig config) {
        request.addHeader("X-Trace-Id", TransportUtil.generateRequestId());
        return true;
    }

    @Override
    public void afterResponse(TransportRequest request, TransportResponse response,
                              OutboundServiceConfig config) {
        // 记录出站调用耗时
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
```

## Nacos 服务发现自动适配

### 工作模式

transport 模块支持两种出站调用寻址模式：

| 模式 | 触发条件 | baseUrl 示例 | 说明 |
|------|----------|-------------|------|
| **硬编码模式（默认）** | 未引入注册中心依赖 | `http://192.168.1.10:8080` | 基础能力库默认行为，直接使用 `OutboundServiceConfig.baseUrl` 中的地址 |
| **服务发现模式** | 引入 `spring-cloud-starter-alibaba-nacos-discovery` | `http://user-service` | 自动生效，通过 Nacos 注册中心解析服务名到实例列表 |

### 自动适配原理

foundation 不引入任何 Nacos 依赖，通过 `@ConditionalOnClass(name = "...")` 字符串守卫实现自动适配：

- **Blocking**：`FeignNacosLoadBalancerAutoConfiguration` 在类路径存在 `com.alibaba.cloud.nacos.NacosDiscoveryProperties` 时自动装配 `BlockingLoadBalancerClient` / `LoadBalancerRequestFactory` / `LoadBalancerInterceptor`
- **Reactive**：`ReactiveNacosLoadBalancerAutoConfiguration` 在同样条件下自动装配 `ReactorLoadBalancerExchangeFilterFunction`

所有 Bean 均使用 `@ConditionalOnMissingBean`，微服务工程可自定义覆盖。

### 微服务工程接入示例

#### 1. 引入 Nacos Discovery 依赖

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

> 该依赖会传递引入 `spring-cloud-starter-loadbalancer`，foundation 的自动适配配置随即生效。

#### 2. 配置 Nacos 注册中心

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
        namespace: public

aryee:
  transport:
    enabled: true
    # 可在代码中通过 TransportProperties POJO 配置服务发现
    # discovery:
    #   enabled: true
    #   type: nacos
    #   prefer-ip: false
```

#### 3. 使用服务名作为 baseUrl

```java
// 服务发现模式下，baseUrl 使用服务名而非 IP:Port
OutboundServiceConfig config = OutboundServiceConfig.builder()
        .serviceName("user-service")
        .baseUrl("http://user-service")   // 通过 Nacos 解析
        .clientType(OutboundClientType.FEIGN)
        .build();

transportService.registerServiceConfig(config);
```

> **注意**：基础能力库本身不包含 Nacos 依赖，独立使用时默认走硬编码模式。只有微服务工程主动引入 Nacos Discovery 后，服务发现模式才会自动激活。

## 装配条件一览

### Blocking Starter（`transport-spring-boot-starter`）

| Bean                              | 装配类                              | 生效条件                                                              |
|-----------------------------------|-------------------------------------|-----------------------------------------------------------------------|
| `blockingInboundFilterChain`      | `AryeeTransportAutoConfiguration` | `aryee.transport.enabled=true`（默认）+ 类路径存在 `DispatcherServlet` + `feign.Client` |
| `blockingOutboundClientFactory`   | `AryeeTransportAutoConfiguration` | 同上                                                                  |
| `blockingOutboundService`         | `AryeeTransportAutoConfiguration` | 同上                                                                  |
| `defaultTransportService`         | `AryeeTransportAutoConfiguration` | 同上                                                                  |
| 熔断装饰器（`BeanPostProcessor`）   | `AryeeTransportAutoConfiguration.CircuitBreakerConfiguration` | `outbound.circuit-breaker.enabled=true` + 类路径存在 Resilience4j |

### Reactive Starter（`transport-reactive-spring-boot-starter`）

| Bean                              | 装配类                              | 生效条件                                                              |
|-----------------------------------|-------------------------------------|-----------------------------------------------------------------------|
| `reactiveInboundFilterChain`      | `AryeeTransportReactiveAutoConfiguration` | `aryee.transport.enabled=true`（默认）+ 类路径存在 `DispatcherHandler` + `WebClient` |
| `reactiveOutboundClientFactory`   | `AryeeTransportReactiveAutoConfiguration` | 同上                                                              |
| `reactiveOutboundService`         | `AryeeTransportReactiveAutoConfiguration` | 同上                                                              |
| `defaultReactiveTransportService` | `AryeeTransportReactiveAutoConfiguration` | 同上                                                              |
| 熔断装饰器（`BeanPostProcessor`）   | `AryeeTransportReactiveAutoConfiguration.CircuitBreakerConfiguration` | `outbound.circuit-breaker.enabled=true` + 类路径存在 Resilience4j |

> 两套 AutoConfiguration 通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册，并通过 `@ConditionalOnClass` 互斥，禁止同时引入两个 Starter。

## 安全管控

传输模块支持可选的安全管控能力，委托 [security 模块](security.md) 进行出站操作权限检查和审计日志，遵循 [security-governance.md](https://github.com/aryeecn/aryee-foundation)（内部规范：security-governance） 规则。

### 工作原理

```
调用方 → SecuredTransportService（装饰器） → 原始 TransportService
              ↓                              ↓
     TransportSecurityService         出站请求/服务配置管理
      ├─ checkPermission()            （委托 security 模块）
      └─ audit()
```

### 安全风险等级

| 操作 | 风险等级 | 权限常量 |
|------|---------|---------|
| `send()` / `sendWithRetry()` / `sendWithTimeout()` | 🔴 高 | `transport:send` |
| `registerServiceConfig()` | 🟠 中 | `transport:manage` |
| `listServiceConfigs()` / `getServiceConfig()` | 🟢 低 | 仅查询，不检查写权限 |

### 配置示例

```yaml
aryee:
  transport:
    security:
      enabled: true      # 启用安全管控
      audit-enabled: true # 启用出站操作审计日志
```

### 使用方式

```java
// 在调用出站请求前设置当前用户
SecurityContextHolder.setUserId(currentUserId);
try {
    transportService.send(request); // 自动权限检查 + 审计日志
} finally {
    SecurityContextHolder.clear();
}
```

### 条件装配

| Bean | 条件 | 说明 |
|------|------|------|
| `DefaultTransportSecurityService` | `DynamicPermissionService` + `SecurityAuditService` Bean 存在 + `security.enabled=true` | 委托 security 模块 |
| `NoopTransportSecurityService` | `security.enabled=true` 但 security 模块未引入 | 降级方案 |
| `SecuredTransportService` (Blocking) | `TransportSecurityService` Bean 存在 | `@Primary` 装饰器 |
| `SecuredReactiveTransportService` (Reactive) | 同上 | `@Primary` 装饰器 |

## 出站熔断

传输模块内置基于 Resilience4j 的出站熔断能力，采用与安全管控一致的**零改造装饰器模式**：启用后通过 `BeanPostProcessor` 自动将 `TransportService` / `ReactiveTransportService` 包装为熔断保护版本，业务代码无需任何修改。

### 工作原理

```
出站请求 → CircuitBreaker[目标服务]
              ├─ CLOSED：正常放行，结果计入滑动窗口统计
              ├─ OPEN：直接返回 503 拒绝响应，不调用下游
              └─ HALF_OPEN：放行少量探测请求，决定恢复或继续熔断

失败判定：抛异常 / success=false / HTTP 状态码 >= 500
熔断维度：按请求路径首段解析下游目标（如 /user-service/api/xxx → user-service），各目标独立熔断
```

### 关键特性

- **零改造接入**：静态 `BeanPostProcessor` 自动包装，与安全装饰器可叠加（熔断位于安全检查之后）
- **按目标隔离**：每个下游服务独立熔断器，单个服务雪崩不拖垮其他调用
- **仅出站生效**：入站请求与配置管理操作直接透传
- **优雅降级**：未引入 Resilience4j 依赖或未开启配置时完全不影响启动

### 启用配置

前置条件：熔断能力为可选依赖，需在业务工程引入（版本由 BOM 管理）：

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-circuitbreaker</artifactId>
</dependency>
```

```yaml
aryee:
  transport:
    outbound:
      circuit-breaker:
        enabled: true                    # 启用熔断
        failure-rate-threshold: 50       # 失败率阈值 50%
        sliding-window-size: 100         # 最近 100 次调用统计窗口
        wait-duration-in-open-state-ms: 30000
```

熔断状态变更会输出 WARN 级别日志（如 `CLOSED_TO_OPEN`），便于监控告警。

## 兼容性

| 环境 | 版本要求 |
|------|----------|
| JDK | 21+ |
| Spring Boot | 4.0.6 |
| Spring Cloud | 2024.0.x |
| Spring WebMVC | 6.x（阻塞式入站） |
| Spring WebFlux | 6.x（响应式入站） |
| OpenFeign | spring-cloud-openfeign（阻塞式出站） |
| WebClient | spring-webflux（响应式出站） |

### 阻塞式 vs 响应式选择指南

#### 使用阻塞式的场景:
- ✅ 传统 Servlet/WebMVC 技术栈
- ✅ 使用 OpenFeign 进行服务间调用
- ✅ 团队熟悉同步编程模型

**典型场景**: 管理后台、普通业务服务

#### 使用响应式的场景:
- ✅ WebFlux 技术栈
- ✅ 使用 WebClient 进行非阻塞 HTTP 调用
- ✅ 高并发、高吞吐场景

**典型场景**: 网关、实时推送、高并发 API
