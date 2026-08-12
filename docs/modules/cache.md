# Aryee Cache 缓存基础设施模块

> **所属项目**: [Aryee Foundation](../../README.md)
> **架构层次**: 基础设施层 (Foundation Layer)
> **技术栈**: Java 21, Spring Boot 4.0.7, Spring Cloud 2025.1.2, Spring Data Redis, Caffeine
> **缓存模式**: 阻塞式 (Blocking) / 响应式 (Reactive)

## 简介

缓存基础设施模块提供了统一的缓存抽象层，基于三层架构（API / Infrastructure / Autoconfigure）实现，支持内存缓存（ConcurrentHashMap）、Caffeine 本地缓存、Redis 分布式缓存以及 Caffeine + Redis 多级缓存四种实现，同时提供阻塞式和响应式两种编程模型，满足不同业务场景的缓存需求。

### 核心特性

- ✅ **三层架构**: 严格遵循 API → Infrastructure → Autoconfigure 分层，依赖方向单向
- ✅ **双模式支持**: 阻塞式 (`CacheService`) 和响应式 (`ReactiveCacheService`) 两种编程模型，接口与实现完全隔离
- ✅ **四种实现**: 纯内存（ConcurrentHashMap）、Caffeine（Window TinyLFU）、Redis 分布式、Caffeine + Redis 多级缓存
- ✅ **多级缓存**: Caffeine（L1）+ Redis（L2）组合，支持 broadcast（Pub/Sub 强一致）/ ttl（短 TTL 最终一致）两种失效策略
- ✅ **统一接口**: 切换实现无需修改业务代码，通过 Starter 二选一引入
- ✅ **丰富数据结构**: String / Hash / Set / List 多种缓存数据结构
- ✅ **声明式缓存**: 基于 `@Cacheable` 注解的声明式缓存，支持 SpEL 表达式
- ✅ **分布式锁**: 提供可重入锁、公平锁、读写锁、信号量、StampedLock 等多种锁注解
- ✅ **缓存三大防护**: 缓存穿透（空值缓存）、缓存击穿（互斥锁）、缓存雪崩（随机 TTL）
- ✅ **可观测性**: 内置审计、监控、安全、失效传播等扩展服务
- ✅ **Spring Boot 自动配置**: 开箱即用，通过 `AutoConfiguration.imports` 注册
- ✅ **智能启动诊断**: 启动时清晰展示缓存类型、Key前缀、TTL随机化状态
- ✅ **错误信息友好化**: Redis 连接失败时提供3条可能原因 + 3条解决方案 + 临时降级建议
- ✅ **配置项智能校验**: 启动时校验 type 值范围、TTL随机系数、互斥锁超时时间

## 模块结构

```
aryee-foundation-cache/
├── cache-api/                                  # API 契约层（无实现）
│   └── cn.aryee.cache.api
│       ├── annotation/                         # 注解定义
│       │   ├── Cacheable.java                  # 声明式缓存注解
│       │   ├── CacheableMeta.java
│       │   └── lock/                           # 分布式锁注解族
│       │       ├── DistributedLockMeta.java    # 锁元注解
│       │       ├── ReentrantLockMeta.java
│       │       ├── FairLockMeta.java
│       │       ├── ReadWriteLockMeta.java
│       │       ├── SemaphoreLockMeta.java
│       │       ├── StampedLockMeta.java
│       │       └── LocalLockMeta.java
│       ├── config/                             # 配置属性
│       │   └── CacheProperties.java            # 配置前缀 aryee.cache
│       ├── constant/                           # 常量
│       ├── enums/                              # 枚举（CacheableMode 等）
│       ├── exception/                          # CacheException
│       ├── listener/                           # CacheEventListener
│       ├── model/                              # CacheEntry / CacheAuditLog / CacheHealthStatus 等
│       ├── service/                            # 服务接口
│       │   ├── CacheService.java               # Blocking 主接口
│       │   ├── ReactiveCacheService.java       # Reactive 主接口
│       │   ├── HashCacheService.java           # Hash 结构接口（Blocking/Reactive 各一）
│       │   ├── SetCacheService.java
│       │   ├── ListCacheService.java
│       │   ├── CacheServiceFactory.java
│       │   ├── ReactiveCacheServiceFactory.java
│       │   ├── CacheAuditService.java          # 审计/安全/可观测/失效 子接口
│       │   ├── CacheSecurityService.java
│       │   ├── CacheObservabilityService.java
│       │   ├── CacheInvalidationService.java
│       │   └── Reactive* 对应接口
│       ├── stats/                              # CacheStatistics
│       └── util/                               # CacheKeyGenerator / ServiceLookup
│
├── cache-infrastructure/                       # 实现层
│   └── cn.aryee.cache.infrastructure
│       ├── blocking/                           # Blocking 实现（禁用 Reactor 类型）
│       │   ├── memory/MemoryCacheServiceImpl.java
│       │   ├── caffeine/CaffeineCacheServiceImpl.java
│       │   ├── redis/RedisCacheServiceImpl.java
│       │   └── multilevel/MultiLevelCacheServiceImpl.java
│       └── reactive/                           # Reactive 实现（禁用阻塞 API）
│           ├── memory/ReactiveMemoryCacheServiceImpl.java
│           ├── caffeine/ReactiveCaffeineCacheServiceImpl.java
│           ├── redis/ReactiveRedisCacheServiceImpl.java
│           └── multilevel/ReactiveMultiLevelCacheServiceImpl.java
│
├── cache-spring-boot-autoconfigure/            # Blocking 自动配置
│   └── cn.aryee.cache.autoconfigure
│       ├── AryeeCacheAutoConfiguration.java
│       ├── spring/AryeeCache.java, AryeeCacheManager.java
│       ├── impl/                               # 内存版扩展服务默认实现
│       └── metrics/CacheMetricsService.java
│
├── cache-reactive-spring-boot-autoconfigure/   # Reactive 自动配置
│   └── cn.aryee.cache.reactive.autoconfigure
│       └── AryeeCacheReactiveAutoConfiguration.java
│
├── cache-spring-boot-starter/                  # Blocking Starter（依赖聚合）
└── cache-reactive-spring-boot-starter/         # Reactive Starter（依赖聚合）
```

**自动配置注册**：
- Blocking: `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` → `cn.aryee.cache.autoconfigure.AryeeCacheAutoConfiguration`
- Reactive: `cn.aryee.cache.reactive.autoconfigure.AryeeCacheReactiveAutoConfiguration`

## 使用方法

### 1. 引入 BOM（推荐）

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>cn.aryee.foundation</groupId>
            <artifactId>bom-full</artifactId>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 2. 按需引入 Starter

```xml
<!-- 阻塞式（Servlet / WebMVC 场景） -->
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>cache-spring-boot-starter</artifactId>
</dependency>

<!-- 或：响应式（WebFlux 场景，二选一，禁止同时引入） -->
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>cache-reactive-spring-boot-starter</artifactId>
</dependency>
```

## 配置

配置前缀：`aryee.cache`（Blocking 与 Reactive 共用同一前缀）

```yaml
aryee:
  cache:
    # 是否启用缓存模块
    enabled: true
    # 默认过期时间（秒）
    default-expiration: 3600
    # 时间单位（SECONDS / MINUTES / HOURS 等）
    time-unit: SECONDS
    # 最大缓存条目数（内存缓存）
    max-size: 10000
    # 缓存键前缀（建议使用应用名）
    key-prefix: ""
    # 是否启用统计功能
    enable-statistics: false
    # 是否启用自动清理过期缓存
    enable-auto-cleanup: true
    # 自动清理间隔（秒）
    cleanup-interval: 300

    # ===== 缓存穿透防护 =====
    # 是否缓存 null 值
    cache-null-values: false
    # null 值过期时间（秒）
    null-value-expiration: 60

    # ===== 缓存雪崩防护 =====
    # 是否开启随机 TTL
    randomize-ttl: false
    # TTL 随机系数：0.0~0.5
    ttl-random-factor: 0.1

    # ===== 缓存击穿防护 =====
    # 是否启用互斥锁
    enable-mutex: false
    # 互斥锁超时时间（秒）
    mutex-timeout: 30
    mutex-time-unit: SECONDS

    # ===== 审计 / 安全 / 可观测 / 失效传播 =====
    audit-enabled: false
    security-encrypt-enabled: false
    security-encrypt-algorithm: AES
    observability-enabled: true
    invalidation-enabled: false
    invalidation-strategy: passive  # passive / active / scheduled

    # ===== 多级缓存（Caffeine L1 + Redis L2）=====
    # 启用后 aryee.cache.type 将被忽略，自动组合 Caffeine + Redis
    # 启用前提：classpath 同时存在 caffeine 与 redis 依赖
    multi-level:
      enabled: false
      # 一致性策略：broadcast（Pub/Sub 通知 L1 失效）/ ttl（L1 短 TTL 兜底）
      strategy: broadcast
      # L1（Caffeine）默认 TTL（秒）
      # broadcast 策略建议 300s，ttl 策略建议 60s
      l1-ttl: 300
      # L1 最大条目数
      l1-max-size: 10000
      # 是否缓存 null 值到 L1（防止穿透）
      l1-cache-null-values: false
      # L1 失效广播通道（仅 broadcast 策略生效）
      broadcast-channel: aryee:cache:invalidation

spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: ""
      database: 0
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

## 代码示例

### Blocking 模式

```java
@Service
public class UserService {

    private final CacheService cacheService;

    public UserService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    // 1. 基础缓存操作
    public User getUserById(Long userId) {
        String key = "user:" + userId;
        User user = cacheService.<User>get(key);
        if (user != null) {
            return user;
        }
        user = userRepository.findById(userId);
        if (user != null) {
            cacheService.set(key, user, Duration.ofHours(1));
        }
        return user;
    }

    // 2. 声明式缓存注解（支持 SpEL）
    @Cacheable(key = "#userId", cacheName = "userCache", expire = 3600)
    public User getUserByIdWithAnnotation(Long userId) {
        return userRepository.findById(userId);
    }

    // 3. 条件缓存 - 只缓存非空结果（防穿透）
    @Cacheable(key = "#email", condition = "#result != null", cacheNull = false)
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // 4. 同步加载 - 防止缓存击穿
    @Cacheable(key = "#productId", sync = true)
    public Product getProductById(Long productId) {
        return productRepository.findById(productId);
    }

    // 5. 原子计数
    public long incrementUserViews(Long userId) {
        return cacheService.increment("user:views:" + userId, 1);
    }

    // 6. 批量查询
    public Map<String, User> getUsersBatch(List<Long> userIds) {
        List<String> keys = userIds.stream().map(id -> "user:" + id).toList();
        return cacheService.<User>multiGet(keys);
    }
}
```

### Reactive 模式

```java
@Service
public class ReactiveUserService {

    private final ReactiveCacheService reactiveCacheService;

    public ReactiveUserService(ReactiveCacheService reactiveCacheService) {
        this.reactiveCacheService = reactiveCacheService;
    }

    public Mono<User> getUserById(Long userId) {
        String key = "user:" + userId;
        return reactiveCacheService.<User>get(key)
                .switchIfEmpty(Mono.defer(() ->
                        userRepository.findById(userId)
                                .flatMap(user -> reactiveCacheService.set(key, user, Duration.ofHours(1))
                                        .thenReturn(user))));
    }

    public Mono<Long> incrementUserViews(Long userId) {
        return reactiveCacheService.increment("user:views:" + userId, 1);
    }

    public Mono<Map<String, User>> getUsersBatch(List<Long> userIds) {
        List<String> keys = userIds.stream().map(id -> "user:" + id).toList();
        return reactiveCacheService.<User>multiGet(keys);
    }
}
```

### 分布式锁注解

```java
@Service
public class DistributedLockService {

    // 可重入锁保护库存扣减
    @ReentrantLockMeta(key = "'stock:' + #productId", timeout = 10)
    public void deductStock(Long productId, int quantity) {
        // 业务逻辑
    }

    // 读写锁 - 读操作
    @ReadWriteLockMeta(key = "'config:' + #configKey", type = ReadWriteLockType.READ)
    public String getConfig(String configKey) {
        return configRepository.getValue(configKey);
    }

    // 读写锁 - 写操作
    @ReadWriteLockMeta(key = "'config:' + #configKey", type = ReadWriteLockType.WRITE)
    public void updateConfig(String configKey, String value) {
        configRepository.setValue(configKey, value);
    }
}
```

### 多级缓存（Caffeine L1 + Redis L2）

启用后 KV 操作会先查询 L1（本地 Caffeine），未命中再查 L2（Redis）并回填 L1；写操作通过 Redis Pub/Sub 通知所有节点失效 L1。业务代码与单级缓存完全一致，无需修改。

**application.yaml 配置示例**：

```yaml
aryee:
  cache:
    # 启用多级缓存后 type 配置会被忽略
    multi-level:
      enabled: true
      # broadcast：Pub/Sub 强一致性；ttl：短 TTL 最终一致性
      strategy: broadcast
      l1-ttl: 300           # L1 默认 5 分钟
      l1-max-size: 10000    # L1 最大条目数
      l1-cache-null-values: false
      broadcast-channel: aryee:cache:invalidation

spring:
  data:
    redis:
      host: localhost
      port: 6379
```

**依赖要求**：classpath 需同时存在 `com.github.ben-manes.caffeine:caffeine` 与 `spring-boot-starter-data-redis`（Blocking）或 `spring-boot-starter-data-redis-reactive`（Reactive）。

**生效条件**：`aryee.cache.multi-level.enabled=true` + Caffeine 类存在 + RedisTemplate/ReactiveRedisTemplate 类存在。

**业务代码示例**（与单级缓存无任何差异）：

```java
@Service
public class UserService {

    private final CacheService cacheService;  // 注入的是 MultiLevelCacheServiceImpl

    public UserService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    public User getUserById(Long userId) {
        String key = "user:" + userId;
        // 自动：L1 命中 → 返回；L1 未命中 → 查 L2 → 回填 L1 → 返回
        User user = cacheService.<User>get(key);
        if (user != null) {
            return user;
        }
        user = userRepository.findById(userId);
        if (user != null) {
            // 写操作：写 L2 → 失效本节点 L1 → Pub/Sub 通知其他节点失效 L1
            cacheService.set(key, user, Duration.ofHours(1));
        }
        return user;
    }
}
```

> **说明**：
> - KV 操作（get/set/delete/multiGet/multiSet/increment/decrement/clear）走 L1+L2 组合
> - Hash/Set/List 操作直接走 L2（结构化数据不在 L1 缓存）
> - broadcast 策略：本节点写操作触发的失效消息会被本节点接收，自动过滤回环
> - ttl 策略：不订阅 Pub/Sub，依赖 L1 短 TTL 兜底，性能更优但一致性稍弱

## 兼容性

| 环境 | 版本要求 |
|------|----------|
| JDK | 21+ |
| Spring Boot | 4.0.7 |
| Spring Cloud | 2025.1.2 |
| Spring Data Redis | 3.5.x |
| Caffeine | 3.x |
| Redis Server | 5.0+ |

### Blocking vs Reactive 选型

- **Blocking 场景**: 传统同步业务、CRUD 操作、Servlet/WebMVC 应用（如管理后台）
- **Reactive 场景**: 高并发高吞吐、非阻塞 I/O、WebFlux 技术栈（如网关、实时推送）

> **重要**: Blocking 与 Reactive Starter 必须二选一，禁止同时引入。
