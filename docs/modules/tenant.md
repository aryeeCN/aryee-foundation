# Aryee Tenant 多租户基础设施模块

> **所属项目**: [Aryee Foundation](../../README.md)
> **技术栈**: Java 21、Spring Boot 4.0.7、JDBC（MyBatis-Plus）、R2DBC

## 简介

多租户基础设施模块基于「共享数据库 + 共享表 + tenant_id」方案，提供统一的多租户解决方案，同时支持 **JDBC 阻塞式** 与 **R2DBC 响应式** 双模式数据访问，确保 SaaS 系统中的数据隔离与安全。

模块遵循 Aryee Foundation 三层架构规范，API 层只定义契约（`TenantService` / `TenantValidator` / `TenantHandler`），Infrastructure 层提供 JDBC（基于 MyBatis-Plus TenantLineHandler）与 R2DBC 双实现。

### 核心特性

- **三种隔离模式**：ROW（行级）/ SCHEMA（Schema 级）/ DATABASE（数据库级）
- **双模式数据访问**：JDBC（MyBatis-Plus 集成）+ R2DBC（响应式查询改写）
- **统一租户上下文**：基于 `ServiceContext` 的 `TenantContextHolder`，支持嵌套切换
- **声明式租户切换**：`@TenantSwitch` 注解 + SpEL 表达式
- **忽略租户隔离**：`@IgnoreTenant` 注解，支持跨租户查询
- **租户生命周期**：激活 / 停用 / 挂起 / 注销 / 过期
- **配额管理**：租户级配额分配与已用量统计
- **租户校验**：状态、过期、有效性校验，支持缓存
- **动态数据源**：基于租户的数据源路由（DATABASE 模式）
- **上下文传递**：Feign / RestTemplate / WebClient 跨服务租户传递
- **租户级缓存隔离**：CacheService 自动代理，缓存键自动添加租户前缀

## 模块结构

```
aryee-foundation-tenant/
├── tenant-api/                                  # API 契约层
├── tenant-infrastructure/                       # 基础设施层（Blocking JDBC + Reactive R2DBC）
├── tenant-spring-boot-autoconfigure/            # 阻塞式自动配置
├── tenant-reactive-spring-boot-autoconfigure/   # 响应式自动配置
├── tenant-spring-boot-starter/                  # 阻塞式 Starter（JDBC）
└── tenant-reactive-spring-boot-starter/         # 响应式 Starter（R2DBC）
```

### 模块说明

| 模块 | artifactId | 说明 |
|------|------------|------|
| API | `tenant-api` | 契约：`TenantService` / `TenantValidator` / `TenantHandler` / `TenantContextHolder`，注解、枚举、配置属性、模型 |
| Infrastructure | `tenant-infrastructure` | 实现：`blocking/jdbc`（MyBatis-Plus 行级处理器、拦截器、Web 过滤器、切面）、`reactive/r2dbc`（响应式上下文、拦截器、查询改写） |
| Autoconfigure | `tenant-spring-boot-autoconfigure` | 阻塞式装配：`AryeeTenantAutoConfiguration` |
| Reactive Autoconfigure | `tenant-reactive-spring-boot-autoconfigure` | 响应式装配：`AryeeTenantReactiveAutoConfiguration` |
| Starter | `tenant-spring-boot-starter` | JDBC 阻塞式依赖聚合（含 `spring-boot-starter-web`） |
| Reactive Starter | `tenant-reactive-spring-boot-starter` | R2DBC 响应式依赖聚合（含 `spring-boot-starter-webflux` + `spring-boot-starter-data-r2dbc` 可选） |

### tenant-api 包结构

```
cn.aryee.tenant.api/
├── annotation/    # @IgnoreTenant、@TenantSwitch
├── context/       # TenantContextHolder（线程安全，支持嵌套切换）
├── enums/         # TenantMode、TenantStatus、TenantType（实现 EnumService<String>）
├── exception/     # TenantException
├── handler/       # TenantHandler 接口
├── model/        # Tenant（继承 Entity<String>）
├── properties/    # TenantProperties（含 CacheConfig 缓存隔离配置）
├── service/       # TenantService、TenantValidator
└── util/          # TenantCacheKeyUtils（租户缓存键构建工具）

cn.aryee.tenant.constant/
└── TenantConstants   # 租户常量
```

### tenant-infrastructure 包结构

```
cn.aryee.tenant.infrastructure/
├── blocking/                            # 阻塞式实现（JDBC）
│   ├── cache/                           # TenantAwareCacheServiceProxyFactory（租户级缓存隔离代理）
│   └── jdbc/
│       ├── aspect/                      # TenantSwitchAdvisor、TenantSwitchInterceptor（@TenantSwitch 切面）
│       ├── filter/                      # TenantWebFilter（Servlet Web 过滤器，从请求头解析租户 ID）
│       ├── handler/                     # AryeeTenantLineHandler（MyBatis-Plus TenantLineHandler）、DefaultTenantHandler、DefaultTenantValidator、TenantInfoProvider
│       ├── interceptor/                 # TenantInterceptor（MyBatis 拦截器，行级 SQL 改写）
│       └── service/                     # DefaultTenantService
└── reactive/                            # 响应式实现（R2DBC）
    ├── cache/                           # TenantAwareReactiveCacheServiceProxyFactory（租户级响应式缓存隔离代理）
    ├── r2dbc/
    │   ├── context/                     # ReactiveTenantContextHolder
    │   ├── filter/                      # ReactiveTenantWebFilter
    │   ├── handler/                     # DefaultReactiveTenantHandler
    │   ├── interceptor/                 # ReactiveTenantInterceptor、TenantR2dbcQueryInterceptor（SQL 改写）
    │   └── resolver/                    # ReactiveTenantResolver
    └── service/                         # ReactiveTenantService、DefaultReactiveTenantService
```

## Maven 依赖

### 阻塞式（JDBC / MyBatis-Plus）

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>tenant-spring-boot-starter</artifactId>
</dependency>
```

### 响应式（R2DBC）

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>tenant-reactive-spring-boot-starter</artifactId>
</dependency>
```

> 二者互斥，根据数据访问技术二选一；版本由 `bom-internal` 统一锁定，无需声明 `<version>`。

## 配置项

配置前缀：`aryee.tenant`，对应 `TenantProperties`。

```yaml
aryee:
  tenant:
    enabled: true                       # 是否启用多租户
    mode: ROW                           # 隔离模式：ROW / SCHEMA / DATABASE
    tenant-id-column: tenant_id         # 租户 ID 字段名（数据库列名）
    ignore-tables:                      # 忽略租户隔离的表
      - sys_tenant
      - sys_dict
      - sys_config
    ignore-sql-ids: []                  # 忽略的 MyBatis Mapper 方法 ID
    default-tenant-id: system           # 默认租户 ID（系统级操作）
    allow-empty-tenant-id: false         # 是否允许租户 ID 为空
    tenant-id-header: X-Tenant-Id       # HTTP 请求头中租户 ID 字段名
    validation-enabled: true            # 是否启用租户校验
    validation-cache-ttl: 300000         # 校验缓存过期时间（毫秒），-1 不缓存
    nested-switch-enabled: true          # 是否启用嵌套租户切换
    ttl-enabled: false                  # 是否启用跨线程传递（TransmittableThreadLocal）
    switch-aspect-enabled: true          # 是否启用 @TenantSwitch 切面
    fallback-to-default-tenant: true     # 未解析到租户 ID 时是否回退默认租户
    cache-null-tenant: false             # 是否缓存空租户结果（防缓存穿透）
    null-cache-ttl: 60000                # 空结果缓存过期时间（毫秒）

    # ====== Schema 级隔离配置 ======
    schema:
      enabled: false                     # 是否启用 Schema 隔离
      prefix: tenant_                    # Schema 命名前缀（{prefix}{tenantId}）
      default-schema: public             # 默认 Schema
      init-ddl-path: classpath:schema/init.sql   # Schema 初始化 DDL
      auto-create: true                  # 租户创建时是否自动创建 Schema

    # ====== 动态数据源配置（DATABASE 模式） ======
    dynamic-datasource:
      enabled: false                     # 是否启用动态数据源
      config-table: sys_tenant_datasource   # 数据源配置表名
      default-datasource: default        # 默认数据源标识
      cache-ttl: 60000                   # 数据源缓存过期时间（毫秒）
      tenant-datasource-mapping:         # 租户 ID -> 数据源标识
        tenant_a: ds_tenant_a

    # ====== 缓存隔离配置 ======
    cache:
      enabled: false                     # 是否启用租户级缓存隔离
      key-prefix: "aryee:tenant:"        # 租户缓存键前缀
      key-separator: ":"                 # 租户ID与业务键分隔符
      ignore-key-prefixes: []            # 忽略租户隔离的缓存键前缀（全局共享缓存）

    # ====== 上下文传递配置 ======
    context:
      feign-enabled: false               # Feign 客户端传递
      rest-template-enabled: false       # RestTemplate 传递
      web-client-enabled: false          # WebClient 传递
      custom-headers: []                 # 自定义请求头
```

## 核心 API

### TenantService（租户服务）

```java
public interface TenantService {
    // 查询
    Tenant getTenantById(String tenantId);
    Tenant getTenantByCode(String tenantCode);
    Tenant getCurrentTenant();
    List<Tenant> listAllTenants();
    List<Tenant> listTenantsByStatus(TenantStatus status);
    List<Tenant> listChildTenants(String parentTenantId);

    // CRUD
    Tenant createTenant(Tenant tenant);
    Tenant updateTenant(Tenant tenant);
    void deleteTenant(String tenantId);

    // 生命周期
    Tenant activateTenant(String tenantId);
    Tenant deactivateTenant(String tenantId);
    Tenant suspendTenant(String tenantId);
    void cancelTenant(String tenantId);

    // 校验
    boolean validateTenant(String tenantId);
    boolean isTenantAccessible(String tenantId);

    // 配额
    boolean checkQuota(String tenantId, Long requiredQuota);
    void increaseUsedQuota(String tenantId, Long delta);
    void decreaseUsedQuota(String tenantId, Long delta);
    Tenant updateQuota(String tenantId, Long quota);
}
```

实现：Blocking `DefaultTenantService`、Reactive `DefaultReactiveTenantService`。

### TenantValidator（租户校验）

```java
public interface TenantValidator {
    boolean exists(String tenantId);
    ValidationResult validate(String tenantId);
    Optional<Tenant> getTenant(String tenantId);   // 走缓存
    void evictCache(String tenantId);

    record ValidationResult(boolean valid, String message, Tenant tenant) {
        public static ValidationResult valid(Tenant tenant);
        public static ValidationResult invalid(String message);
    }
}
```

### TenantHandler（租户 ID 处理器）

```java
public interface TenantHandler {
    String getTenantId();
    void setTenantId(String tenantId);
    void clearTenantId();
    default boolean isValidTenantId(String tenantId);
}
```

实现：Blocking `DefaultTenantHandler`、Reactive `DefaultReactiveTenantHandler`，从 HTTP 请求头 / 会话 / 上下文获取租户 ID。

### TenantContextHolder（租户上下文持有者）

```java
public class TenantContextHolder {
    public static void setTenantId(String tenantId);
    public static String getTenantId();
    public static void setTenantCode(String tenantCode);
    public static String getTenantCode();
    public static void setTenantStatus(TenantStatus status);
    public static void setTenantContext(Tenant tenant);   // 一次性加载完整上下文

    public static void setIgnore(boolean ignore);
    public static boolean isIgnore();

    // 嵌套安全的租户切换
    public static void executeWithTenant(String tenantId, Runnable runnable);
    public static <T> T executeWithTenant(String tenantId, Callable<T> callable) throws Exception;
    public static void executeWithoutTenant(Runnable runnable);   // 忽略租户隔离执行
    public static <T> T executeWithoutTenant(Callable<T> callable) throws Exception;

    public static void clear();
}
```

底层基于 `cn.aryee.commons.context.ServiceContext`，自动支持 Servlet 与 WebFlux 上下文。

## 使用示例

### 1. 声明式租户切换（注解）

```java
@Service
public class OrderService {

    @TenantSwitch(tenantId = "#tenantId", restoreAfter = true)
    public Order processOrder(String tenantId, OrderRequest request) {
        // 方法内租户上下文自动切换为 #tenantId
        // 方法执行完毕后自动恢复原上下文
        return orderRepository.save(request);
    }

    @IgnoreTenant   // 忽略租户隔离，查询全量数据
    public List<SysConfig> listAllConfigs() {
        return configRepository.findAll();
    }
}
```

### 2. 编程式租户切换

```java
@Service
public class TenantOperationService {

    public void crossTenantOperation(String tenantId) {
        // 嵌套安全：自动保存与恢复上层租户
        TenantContextHolder.executeWithTenant(tenantId, () -> {
            // 此处查询会自动加上 tenant_id = ? 条件
            orderRepository.findByStatus("PAID");
        });
    }

    public void systemLevelOperation() {
        // 跨租户执行（如系统初始化、数据迁移）
        TenantContextHolder.executeWithoutTenant(() -> {
            userRepository.deleteAll();
        });
    }

    public String currentTenantId() {
        return TenantContextHolder.getTenantId();
    }
}
```

### 3. Reactive 模式

```java
@Service
public class ReactiveOrderService {

    @Autowired
    private ReactiveTenantService tenantService;

    public Mono<Order> findOrder(String orderId) {
        // R2DBC 查询自动改写，附加 tenant_id 条件
        return orderRepository.findById(orderId);
    }

    public Mono<Tenant> currentTenant() {
        return tenantService.getCurrentTenant();
    }
}
```

`ReactiveTenantWebFilter` 自动从请求头 `X-Tenant-Id` 解析租户 ID，写入 `ReactiveTenantContextHolder`；`TenantR2dbcQueryInterceptor` 自动改写 SQL，附加 `tenant_id = ?` 条件。

### 4. 租户管理（CRUD + 生命周期）

```java
@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    @Autowired
    private TenantService tenantService;

    @PostMapping
    public Tenant create(@RequestBody Tenant tenant) {
        return tenantService.createTenant(tenant);
    }

    @PutMapping("/{tenantId}/activate")
    public Tenant activate(@PathVariable String tenantId) {
        return tenantService.activateTenant(tenantId);
    }

    @PutMapping("/{tenantId}/quota")
    public Tenant updateQuota(@PathVariable String tenantId, @RequestParam Long quota) {
        return tenantService.updateQuota(tenantId, quota);
    }

    @GetMapping("/{tenantId}/accessible")
    public boolean isAccessible(@PathVariable String tenantId) {
        return tenantService.isTenantAccessible(tenantId);
    }
}
```

### 5. 租户级缓存隔离

启用 `aryee.tenant.cache.enabled=true` 后，所有 `CacheService` / `ReactiveCacheService` 的操作自动在缓存键中添加租户前缀，**无需修改任何业务代码**。

```yaml
aryee:
  tenant:
    cache:
      enabled: true
      key-prefix: "aryee:tenant:"
      ignore-key-prefixes:
        - "aryee:platform:config:"   # 全局共享配置不被租户隔离
```

**工作原理**：

- 自动配置通过 `BeanPostProcessor` 将 `CacheService` Bean 包装为 JDK 动态代理
- 代理拦截所有缓存操作，将 `key` 改写为 `{prefix}{tenantId}:{originalKey}`
- 例如：租户 `T001` 调用 `cacheService.get("user:123")` 实际访问 `aryee:tenant:T001:user:123`
- 支持 `ignore-key-prefixes` 配置全局共享缓存（不添加租户前缀）
- 同时支持 Blocking（`CacheService`）和 Reactive（`ReactiveCacheService`）模式

**手动构建租户缓存键**（特殊场景）：

```java
import cn.aryee.tenant.api.util.TenantCacheKeyUtils;

// 手动构建带租户前缀的缓存键
String tenantKey = TenantCacheKeyUtils.buildTenantKey("user:123", tenantProperties);
// 结果: "aryee:tenant:T001:user:123"
```

### 6. 数据模型

`Tenant` 继承 `Entity<String>`，包含审计字段（createTime / updateTime / creator / updater / deleted / version）：

| 字段 | 类型 | 说明 |
|------|------|------|
| tenantCode | String | 租户编码（业务唯一标识） |
| tenantName | String | 租户名称 |
| status | TenantStatus | 状态：ACTIVE / INACTIVE / SUSPENDED / EXPIRED / CANCELLED |
| tenantType | TenantType | 租户类型 |
| parentTenantId | String | 父租户 ID（多级租户） |
| tenantLevel | Integer | 租户层级（0=顶级） |
| domain | String | 自定义域名 |
| quota | Long | 配额总量（-1 无限制） |
| usedQuota | Long | 已使用配额 |
| expireTime | Long | 过期时间戳（null 永不过期） |

## 自动装配

### 阻塞式

`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`：

- `cn.aryee.tenant.autoconfigure.AryeeTenantAutoConfiguration`：装配 `DefaultTenantService` / `DefaultTenantHandler` / `DefaultTenantValidator` / `AryeeTenantLineHandler` / `TenantWebFilter` / `TenantInterceptor` / `TenantSwitchAdvisor`；当 `cache-api` 在 classpath 且 `aryee.tenant.cache.enabled=true` 时，自动包装 `CacheService` 为租户感知代理

### 响应式

- `cn.aryee.tenant.reactive.autoconfigure.AryeeTenantReactiveAutoConfiguration`：装配 `DefaultReactiveTenantService` / `DefaultReactiveTenantHandler` / `ReactiveTenantWebFilter` / `ReactiveTenantInterceptor` / `TenantR2dbcQueryInterceptor`；当 `cache-api` 在 classpath 且 `aryee.tenant.cache.enabled=true` 时，自动包装 `ReactiveCacheService` 为租户感知代理

所有 Bean 使用 `@ConditionalOnMissingBean` 装配，可通过自定义 `@Bean` 覆盖默认实现。

## 隔离模式选择建议

| 模式 | 适用场景 | 配置 |
|------|---------|------|
| `ROW` | 大量租户、数据量适中、运维成本敏感（如 SaaS 中小企业平台） | `aryee.tenant.mode=ROW` |
| `SCHEMA` | 中等数量租户、隔离要求高、需租户级备份恢复（如金融级应用） | `aryee.tenant.mode=SCHEMA` + `schema.enabled=true` |
| `DATABASE` | 少量租户、隔离要求极高、单租户数据量大 | `aryee.tenant.mode=DATABASE` + `dynamic-datasource.enabled=true` |

## 兼容性

| 技术组件 | 版本要求 |
|---------|---------|
| Java | 21+ |
| Spring Boot | 4.0.7 |
| MyBatis-Plus | 3.5.x（Blocking 模式行级隔离） |
| R2DBC | Spring Data R2DBC 1.x（Reactive 模式 SQL 改写） |
| MySQL | 8.0+ |
| PostgreSQL | 12+ |
| TransmittableThreadLocal | 可选，跨线程传递 |
| cache-api | 可选，租户级缓存隔离（自动代理 CacheService） |
