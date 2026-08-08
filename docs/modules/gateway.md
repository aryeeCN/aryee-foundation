# Aryee Gateway API 网关增强模块

> **所属项目**: [Aryee Foundation](../../README.md)
> **技术栈**: Java 21、Spring Boot 4.0.7、Spring Cloud Gateway、Nacos、Security API（认证/签名委托）

## 简介

API 网关增强模块构建于 Spring Cloud Gateway 之上，提供 **限流 / 熔断 / 灰度 / 聚合** 四大核心能力，以及 **统一鉴权 / Same-Token / SQL 注入 / API 签名 / 访问日志** 五类可复用过滤器。其中鉴权与签名验证委托 [security-api](security.md) 的 `ReactiveAuthService` / `ReactiveSignatureService` 接口实现，底层认证方案（Sa-Token / JWT 等）由 security 模块决定，网关层不再直接耦合第三方安全框架。

模块同时提供 Blocking 与 Reactive 双模式 Starter，Reactive 模式深度集成 Spring Cloud Gateway 的 `GlobalFilter` 与 `GatewayFilter`。

### 核心特性

- **限流**：令牌桶（TokenBucket）+ 漏桶（LeakyBucket）双算法，支持 Reactive `KeyResolver`
- **熔断**：基于 Resilience4j 的熔断器，CLOSED / OPEN / HALF_OPEN 三态切换
- **灰度发布**：加权路由 + 用户级路由，支持动态权重调整
- **API 聚合**：多接口并行 / 串行聚合，支持超时控制
- **统一鉴权过滤器**：委托 `ReactiveAuthService.verifyToken()` 验证令牌，支持 Authorization Bearer 和 satoken 请求头
- **安全上下文传播**：鉴权成功后自动解析 Token 中的 userId/tenantId，写入 Reactor Context 和 `X-User-Id`/`X-Tenant-Id` 请求头，供下游微服务恢复安全上下文
- **Same-Token 转发过滤器**：自动注入 Same-Token，防止绕过网关直连微服务
- **SQL 注入拦截过滤器**：路径 / 参数 / 请求体 SQL 注入检测
- **API 签名验证过滤器**：委托 `ReactiveSignatureService` 验证签名 + nonce 防重放，Redis / 内存双存储
- **访问日志过滤器**：请求方法、路径、来源 IP、响应状态码、耗时记录
- **动态路由**：基于 Spring Cloud Gateway 的 `DynamicRouteService`，支持 Nacos 配置中心
- **服务网格集成**：Istio / Linkerd 平台适配
- **CORS 与 XSS 全局过滤器**：内置跨域与 XSS 防护
- **统一异常体系**：`Resilience4jCircuitBreaker` 在熔断/降级失败时抛出 `SystemException`，与全局异常体系对齐

## 模块结构

```
aryee-foundation-gateway/
├── gateway-api/                                # API 契约层
├── gateway-infrastructure/                     # 基础设施层（Blocking + Reactive）
├── gateway-spring-boot-autoconfigure/          # 阻塞式自动配置
├── gateway-reactive-spring-boot-autoconfigure/ # 响应式自动配置（含全局过滤器）
├── gateway-spring-boot-starter/                # 阻塞式 Starter
└── gateway-reactive-spring-boot-starter/       # 响应式 Starter（含 Spring Cloud Gateway 可选依赖）
```

### 模块说明

| 模块 | artifactId | 说明 |
|------|------------|------|
| API | `gateway-api` | 限流 / 熔断 / 灰度 / 聚合 / 服务网格接口契约，及五类过滤器配置属性 |
| Infrastructure | `gateway-infrastructure` | Blocking 实现（TokenBucket / LeakyBucket / Resilience4j / WeightedCanary / DefaultApiAggregator）+ Reactive 全局过滤器（AuthGateway / SameToken / SqlInjection / ApiSignature / AccessLog）+ Istio / Linkerd 适配，依赖 `security-api` 契约层 |
| Autoconfigure | `gateway-spring-boot-autoconfigure` | 阻塞式装配：限流 / 熔断 / 灰度 / 聚合 / 服务网格 |
| Reactive Autoconfigure | `gateway-reactive-spring-boot-autoconfigure` | 响应式装配：GatewayReactiveAutoConfiguration + CorsAutoConfiguration + RateLimitKeyResolverAutoConfiguration + DynamicRouteAutoConfiguration |
| Starter | `gateway-spring-boot-starter` | 阻塞式依赖聚合 |
| Reactive Starter | `gateway-reactive-spring-boot-starter` | 响应式依赖聚合，可选引入 Spring Cloud Gateway、Sa-Token Reactor、Reactive Redis |

### gateway-api 包结构

```
cn.aryee.gateway.api/
├── aggregate/    # ApiAggregator、AggregateRequest、AggregateResponse、ExecutionMode
├── canary/       # CanaryRouter、CanaryConfig、RouteDestination、RequestContext
├── circuit/      # CircuitBreaker、CircuitBreakerConfig、CircuitState、CircuitMetrics
├── cors/         # CorsProperties
├── filter/       # GatewayFilterProperties（五类过滤器统一配置）、SignatureProperties、SqlInjectionProperties、XssProperties、ForwardAuthProperties
├── mesh/         # MeshService、MeshProvider、TrafficRule、CircuitBreakerConfig、MTLSConfig
├── model/        # GatewayRoute
├── rate/         # RateLimiter、RateLimitConfig、RateLimitResult
└── route/        # DynamicRouteService、RouteInfo
```

### gateway-infrastructure 包结构

```
cn.aryee.gateway.infrastructure/
├── blocking/                  # 阻塞式实现
│   ├── aggregate/             # DefaultApiAggregator
│   ├── canary/                # WeightedCanaryRouter
│   ├── circuit/               # Resilience4jCircuitBreaker
│   ├── rate/                  # TokenBucketRateLimiter / LeakyBucketRateLimiter
│   └── BlockingGatewayService.java
├── mesh/                      # 服务网格（Istio / Linkerd）
│   ├── blocking/              # BlockingMeshService、IstioMeshService、LinkerdMeshService
│   └── reactive/              # ReactiveMeshService
└── reactive/                  # 响应式实现
    ├── filter/                # AuthGatewayFilter（含安全上下文传播）、SameTokenForwardFilter、SqlInjectionFilter、ApiSignatureFilter、AccessLogFilter、NonceStore（InMemory / Redis）
    └── ReactiveGatewayService.java
```

## Maven 依赖

### 阻塞式

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>gateway-spring-boot-starter</artifactId>
</dependency>
```

### 响应式（Spring Cloud Gateway 场景）

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>gateway-reactive-spring-boot-starter</artifactId>
</dependency>
```

Reactive Starter 已 `optional` 引入 `spring-cloud-starter-gateway-server-webflux`、`sa-token-reactor-spring-boot4-starter`（Same-Token 转发用）、`spring-boot-starter-data-redis-reactive`，按需在业务工程中显式声明。`gateway-infrastructure` 已依赖 `security-api` 契约层，鉴权与签名验证委托 security 模块的 `ReactiveAuthService` / `ReactiveSignatureService` 实现。

## 配置项

配置前缀：`aryee.gateway`，对应 `GatewayProperties` / `GatewayReactiveProperties`。

```yaml
aryee:
  gateway:
    enabled: true                  # 是否启用网关增强

    rate-limit:                    # 限流配置
      enabled: false               # 是否启用
      algorithm: tokenBucket       # tokenBucket 或 leakyBucket

    circuit-breaker:               # 熔断配置
      enabled: false
      type: resilience4j

    canary:                        # 灰度发布配置
      enabled: false

    aggregate:                     # API 聚合配置
      enabled: false

    # ====== 五类可复用过滤器配置（GatewayFilterProperties，前缀 aryee.gateway.filter） ======
    filter:
      sa-token:                    # 统一鉴权（委托 ReactiveAuthService）
        enabled: false             # 默认关闭，需 security 模块提供 ReactiveAuthService Bean
        exclude-path-list:         # 完全排除路径（Ant 风格）
          - /auth/login
        not-match-pattern-list:    # 经过过滤器但跳过登录校验
          - /anon/**

      same-token:                  # Same-Token 防绕过转发
        enabled: false
        exclude-pattern-list:
          - /actuator/**

      sql-injection:               # SQL 注入拦截（默认开启）
        enabled: true
        exclude-path-list:
          - /api-docs/**

      api-signature:               # API 签名验证（委托 ReactiveSignatureService）
        enabled: false             # 默认关闭，需 security 模块提供 ReactiveSignatureService Bean
        exclude-path-list:
          - /anon/public/**
        timestamp-window: 300000  # 时间戳有效窗口（毫秒，默认 5 分钟）
        credentials:               # AccessKey -> SecretKey 映射
          AK123: SK123

      access-log:                  # 访问日志（默认开启）
        enabled: true
        exclude-path-list:
          - /actuator/**
```

## 核心 API

### RateLimiter（限流）

```java
public interface RateLimiter {
    RateLimitResult tryAcquire(String key);
    RateLimitResult tryAcquire(String key, int permits);
    RateLimitConfig getConfig();
    void reset(String key);
    long getRemaining(String key);
}
```

实现：`TokenBucketRateLimiter`（按速率补充令牌，允许突发）、`LeakyBucketRateLimiter`（按固定速率处理，平滑流量）。

### CircuitBreaker（熔断）

```java
public interface CircuitBreaker {
    <T> T execute(SupplierWithException<T> supplier, SupplierWithException<T> fallback);
    void execute(RunnableWithException runnable, RunnableWithException fallback);
    CircuitState getState();
    void reset();
    CircuitMetrics getMetrics();
}
```

实现：`Resilience4jCircuitBreaker`，支持 COUNT_BASED / TIME_BASED 滑动窗口、慢调用比例阈值、半开状态请求数等。

### CanaryRouter（灰度发布）

```java
public interface CanaryRouter {
    RouteDestination route(RequestContext requestContext);
    CanaryConfig getConfig();
    void updateWeight(int weight);
    boolean isCanaryUser(String userId);
    Set<String> getCanaryUsers();
    void addCanaryUser(String userId);
    void removeCanaryUser(String userId);
}
```

实现：`WeightedCanaryRouter`，支持基于权重和基于用户列表的灰度路由。

### ApiAggregator（API 聚合）

```java
public interface ApiAggregator {
    AggregateResponse aggregate(List<AggregateRequest> requests);
    AggregateResponse aggregate(List<AggregateRequest> requests, long timeoutMs);
    ApiResponse execute(AggregateRequest request);
    AggregateConfig getConfig();
}
```

实现：`DefaultApiAggregator`，支持 `ExecutionMode.PARALLEL`（并行）与 `ExecutionMode.SEQUENTIAL`（串行）。

### DynamicRouteService（动态路由）

```java
public interface DynamicRouteService {
    boolean addRoute(RouteInfo routeInfo);
    boolean updateRoute(RouteInfo routeInfo);
    boolean deleteRoute(String routeId);
    List<RouteInfo> listRoutes();
    void refreshRoutes();
}
```

Reactive 实现位于 `cn.aryee.gateway.reactive.autoconfigure.filter.DynamicRouteServiceImpl`，基于 Spring Cloud Gateway 的 `RouteDefinitionWriter` 与 `RouteDefinitionLocator`，可对接 Nacos 配置中心。

### MeshService（服务网格）

```java
public interface MeshService {
    String getProvider();                                    // ISTIO / LINKERD
    void applyTrafficRule(TrafficRule rule);
    List<TrafficRule> getTrafficRules(String serviceName);
    void deleteTrafficRule(String ruleName);
    void configureCircuitBreaker(String serviceName, CircuitBreakerConfig config);
    void applyFaultInjection(String serviceName, FaultInjectionConfig config);
    void configureMTLS(String serviceName, MTLSConfig config);
    boolean isReady();
}
```

实现：`IstioMeshService`、`LinkerdMeshService`，通过 `MeshProvider` 枚举选择。

## 使用示例

### 1. 限流

```java
@Service
public class ApiRateLimitService {

    @Autowired
    private RateLimiter rateLimiter;

    public void processRequest(String apiPath) {
        RateLimitResult result = rateLimiter.tryAcquire(apiPath);
        if (!result.isAllowed()) {
            throw new BusinessException(ErrorCodeEnum.OPERATION_NOT_ALLOWED, "请求过于频繁");
        }
        doProcess();
    }
}
```

### 2. 熔断 + 降级

```java
@Service
public class RemoteCallService {

    @Autowired
    private CircuitBreaker circuitBreaker;

    public String callUserService() {
        return circuitBreaker.execute(
            () -> userClient.getUser(),
            () -> "fallback-user"   // 降级返回
        );
    }
}
```

### 3. 灰度发布

```java
@Service
public class CanaryService {

    @Autowired
    private CanaryRouter canaryRouter;

    public RouteDestination route(RequestContext ctx) {
        return canaryRouter.route(ctx);
    }

    public void increaseCanaryWeight(int weight) {
        canaryRouter.updateWeight(weight);   // 动态调整灰度权重
    }
}
```

### 4. API 聚合

```java
@Service
public class DashboardService {

    @Autowired
    private ApiAggregator apiAggregator;

    public AggregateResponse getUserDashboard(String userId) {
        List<AggregateRequest> requests = List.of(
            AggregateRequest.builder().name("userInfo")
                .url("http://user-service/api/users/" + userId).method("GET").required(true).build(),
            AggregateRequest.builder().name("orders")
                .url("http://order-service/api/orders?userId=" + userId).method("GET").required(false).build()
        );
        return apiAggregator.aggregate(requests);
    }
}
```

### 5. 网关过滤器链（Reactive 模式自动装配）

引入 `gateway-reactive-spring-boot-starter` 后，以下过滤器按配置自动装配为 Spring Cloud Gateway 的 `GlobalFilter`：

| 过滤器 | 触发条件 | 默认 |
|--------|---------|------|
| `AuthGatewayFilter` | `ReactiveAuthService` Bean 存在且 `sa-token.enabled=true` | 关闭 |
| **AuthGatewayFilter 安全上下文传播** | 鉴权成功后自动注入 `X-User-Id`/`X-Tenant-Id` 请求头 | 启用 |
| `SameTokenForwardFilter` | `aryee.gateway.filter.same-token.enabled=true` 且 `SaSameUtil` 类存在 | 关闭 |
| `SqlInjectionFilter` | Spring Cloud Gateway 存在且 `sql-injection.enabled != false` | 开启 |
| `ApiSignatureFilter`（Redis） | `ReactiveSignatureService` Bean + Reactive Redis 存在且 `api-signature.enabled=true` | 关闭 |
| `ApiSignatureFilter`（内存） | 上一条未装配且 `api-signature.enabled=true` | 关闭 |
| `AccessLogFilter` | Spring Cloud Gateway 存在且 `access-log.enabled != false` | 开启 |
| `CacheBodyGlobalFilter` | 始终装配，缓存请求体供后续过滤器多次读取 | 开启 |
| `XssGlobalFilter` | 始终装配，XSS 清洗 | 开启 |

### 6. 安全上下文传播

`AuthGatewayFilter` 鉴权成功后，自动解析 Token 中的用户信息并传播安全上下文：

```
用户请求 → AuthGatewayFilter
    ↓
1. 解析 Authorization / satoken 请求头获取 Token
2. 调用 ReactiveAuthService.verifyToken(token) 验证
3. 调用 ReactiveAuthService.parseToken(token) 解析 claims
4. 提取 userId / tenantId
5. 写入 Reactor Context（供网关内部使用）
6. 注入请求头 X-User-Id / X-Tenant-Id（供下游微服务恢复上下文）
    ↓
下游微服务 → SecurityContextInboundFilter / ReactiveSecurityContextInboundFilter
    ↓
读取请求头 → 写入 SecurityContextHolder → 业务代码可直接获取
```

下游微服务无需额外配置，只需引入 security starter 即可自动恢复上下文：

```yaml
# 微服务 application.yml
aryee:
  security:
    inbound-enabled: true   # 默认已开启，自动从请求头恢复上下文
```

## 自动装配

### 阻塞式

`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`：

- `cn.aryee.gateway.autoconfigure.GatewayAutoConfiguration`
- `cn.aryee.gateway.autoconfigure.mesh.MeshAutoConfiguration`

### 响应式

- `cn.aryee.gateway.reactive.autoconfigure.GatewayReactiveAutoConfiguration`：装配限流 / 熔断 / 灰度 / 聚合 Bean 与五类过滤器（鉴权/签名通过 `@ConditionalOnBean` 依赖 security 模块提供的 `ReactiveAuthService` / `ReactiveSignatureService`）
- `cn.aryee.gateway.reactive.autoconfigure.CorsAutoConfiguration`：跨域配置
- `cn.aryee.gateway.reactive.autoconfigure.RateLimitKeyResolverAutoConfiguration`：Reactive `KeyResolver`
- `cn.aryee.gateway.reactive.autoconfigure.DynamicRouteAutoConfiguration`：动态路由

所有 Bean 使用 `@ConditionalOnMissingBean` 装配，可通过自定义 `@Bean` 覆盖。

## 兼容性

| 技术组件 | 版本要求 |
|---------|---------|
| Java | 21+ |
| Spring Boot | 4.0.7 |
| Spring Cloud Gateway | 2025.1.2 |
| Resilience4j | 2.x |
| security-api | 项目内部模块（提供 ReactiveAuthService / ReactiveSignatureService） |
| Sa-Token | 1.45.0（仅 Same-Token 转发过滤器需要） |
| Nacos | 2.x（动态路由配置中心） |
| Reactive Redis | Spring Data Redis Reactive（API 签名防重放） |
