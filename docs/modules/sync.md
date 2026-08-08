# Aryee Sync 数据同步与分布式锁模块

> **所属项目**: [Aryee Foundation](../../README.md)
> **架构层次**: 基础设施层 (Foundation Layer)
> **技术栈**: Java 21, Spring Boot 4.0.6, Spring Data Redis（可选）, Reactor（可选）
> **同步模式**: 阻塞式 (Blocking), 响应式 (Reactive)

## 简介

数据同步与分布式锁基础设施模块，提供两类核心能力：

1. **数据同步**：通过 `DataSyncService` / `ReactiveDataSyncService` 接口，统一抽象同步任务（`SyncTask`）与同步数据（`SyncData`）的提交、执行、查询、删除等生命周期管理；默认提供内存实现，并支持基于策略（Database / MessageQueue / Scheduled）扩展
2. **分布式锁**：通过 `DistributedLock` 接口提供 `lock` / `tryLock` / `unlock` 等锁语义，内置 `RedisDistributedLock`（基于 Redis + Lua 脚本 + 看门狗续期）与 `DistributedLockUtil`（本地 `ReentrantLock` 兜底）

### 核心特性

- ✅ **双模式同步**：阻塞式 `DataSyncService`（同步 / 批量 / 异步）与响应式 `ReactiveDataSyncService`（基于 `Mono` / `Flux`，非阻塞）
- ✅ **同步任务管理**：创建 / 执行 / 查询 / 更新 / 删除 / 批量删除 / 统计 / 清空
- ✅ **可扩展策略**：`DataSyncStrategyFactory` + `DatabaseSyncStrategy` / `MessageQueueSyncStrategy` / `ScheduledSyncStrategy`
- ✅ **Redis 分布式锁**：基于 `SET NX PX` + Lua 脚本解锁 + 看门狗自动续期，避免误删他人锁
- ✅ **本地锁兜底**：`DistributedLockUtil` 在未引入 Redis 时使用 `ReentrantLock` 进程内锁
- ✅ **SPI 服务发现**：通过 `DataSyncServiceFactory` + `META-INF/services` 实现可插拔
- ✅ **安全上下文透传**：异步同步时自动捕获并恢复 `SecurityContextHolder`（userId / tenantId），确保审计日志等安全功能在异步线程中正常工作
- ✅ **统一异常体系**：`RedisDistributedLock` 与 `DefaultDataSyncService` 采用 `SystemException` 替代 `RuntimeException`，与全局异常体系保持一致

## 架构定位

```
aryee-foundation-sync/                            ← 聚合 POM（packaging=pom）
├── sync-api/                                     ← 契约层
│   ├── annotation/    @DataSync, @SyncStrategy
│   ├── config/        SyncProperties (prefix = aryee.sync)
│   ├── constant/      SyncConstants
│   ├── model/         SyncData, SyncTask, SyncStatus
│   ├── service/       DataSyncService, ReactiveDataSyncService,
│   │                  DataSyncStrategy, ReactiveDataSyncStrategy,
│   │                  DataSyncServiceFactory, ReactiveDataSyncServiceFactory
│   └── util/          DataSyncServiceLookup
│
├── sync-infrastructure/                          ← 实现层
│   ├── blocking/
│   │   ├── lock/                          ← 分布式锁
│   │   │   ├── DistributedLock              接口
│   │   │   ├── DistributedLockUtil         本地 ReentrantLock 兜底 + Redis 锁工厂
│   │   │   └── RedisDistributedLock        Redis SET NX PX + Lua 解锁 + 看门狗续期
│   │   ├── memory/                        ← 内存实现
│   │   │   ├── InMemoryDataSyncService
│   │   │   └── InMemoryDataSyncServiceFactory
│   │   ├── service/                       ← 默认实现
│   │   │   ├── DefaultDataSyncService
│   │   │   └── DefaultDataSyncServiceFactory
│   │   └── strategy/                      ← 同步策略
│   │       ├── DataSyncStrategyFactory
│   │       ├── DatabaseSyncStrategy
│   │       ├── MessageQueueSyncStrategy
│   │       └── ScheduledSyncStrategy
│   └── reactive/memory/                   ← 响应式内存实现
│       ├── ReactiveInMemoryDataSyncService
│       └── ReactiveInMemoryDataSyncServiceFactory
│   （META-INF/services/cn.aryee.foundation.sync.api.service.DataSyncServiceFactory）
│
├── sync-spring-boot-autoconfigure/               ← Blocking 自动装配
│   └── AryeeSyncAutoConfiguration
│       （META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports）
│
├── sync-spring-boot-starter/                    ← Blocking Starter 依赖聚合
│
├── sync-reactive-spring-boot-autoconfigure/     ← Reactive 自动装配
│   └── SyncReactiveAutoConfiguration
│       （包名: cn.aryee.sync.reactive.autoconfigure）
│
└── sync-reactive-spring-boot-starter/           ← Reactive Starter 依赖聚合
```

- **依赖方向**: Starter → Autoconfigure → Infrastructure → API
- **基础设施依赖**: `sync-infrastructure` 模块依赖 `commons-core`，使用其统一异常体系（`SystemException`）及公共工具
- **可选依赖**: `spring-boot-starter-data-redis`、`reactor-core`（均 `optional`，由使用方按需引入）
- **双模式隔离**（参见 architecture.md §6.1）：Blocking 与 Reactive 使用独立的 Starter / Autoconfigure 模块，用户二选一引入，禁止同时引入

## Maven 依赖

### Blocking 模式（默认，Servlet/WebMVC 场景）

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>sync-spring-boot-starter</artifactId>
    <!-- 版本由 bom-full 统一管理，无需指定 -->
</dependency>
```

如需使用 Redis 分布式锁，请额外引入：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### Reactive 模式（WebFlux 场景）

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>sync-reactive-spring-boot-starter</artifactId>
</dependency>
```

> Reactive Starter 已聚合 `spring-boot-starter-webflux`，自动触发 `@ConditionalOnClass(name = "reactor.core.publisher.Mono")` 守卫。

## 配置说明

配置前缀：`aryee.sync`（`SyncProperties`，Blocking / Reactive 共享）

```yaml
aryee:
  sync:
    # 是否启用同步模块，默认 true
    enabled: true
    # 异步同步线程池大小
    async-pool-size: 4
    # 是否开启统计
    enable-statistics: false
    # 默认重试次数
    retry-count: 0
    # 默认重试间隔（毫秒）
    retry-delay: 1000
```

## 使用示例

### 1. Blocking 数据同步

```java
import cn.aryee.sync.api.model.SyncData;
import cn.aryee.sync.api.model.SyncTask;
import cn.aryee.sync.api.service.DataSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SyncSampleService {

    @Autowired
    private DataSyncService dataSyncService;

    public void syncOne() {
        SyncData data = new SyncData("USER", payload);
        boolean ok = dataSyncService.sync(data);
    }

    public void syncBatch() {
        List<SyncData> list = List.of(new SyncData("USER", u1), new SyncData("USER", u2));
        List<Boolean> results = dataSyncService.syncBatch(list);
    }

    public void syncAsync() {
        // 异步提交，不阻塞当前线程
        // 自动捕获当前 SecurityContextHolder，在异步线程中恢复 userId/tenantId
        dataSyncService.syncAsync(new SyncData("ORDER", payload));
    }

    public String submitTask() {
        SyncTask task = new SyncTask();
        task.setTaskName("user-sync");
        task.setSyncData(new SyncData("USER", payload));
        String taskId = dataSyncService.createSyncTask(task);
        dataSyncService.executeSyncTask(taskId);
        return taskId;
    }
}
```

### 2. Reactive 数据同步

```java
import cn.aryee.sync.api.model.SyncData;
import cn.aryee.sync.api.model.SyncTask;
import cn.aryee.sync.api.service.ReactiveDataSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ReactiveSyncSampleService {

    @Autowired
    private ReactiveDataSyncService reactiveDataSyncService;

    public Mono<Boolean> syncOne() {
        return reactiveDataSyncService.sync(new SyncData("USER", payload));
    }

    public Mono<Void> asyncFireAndForget() {
        return reactiveDataSyncService.syncAsync(new SyncData("USER", payload));
    }

    public Flux<Boolean> syncBatch(Flux<SyncData> stream) {
        return reactiveDataSyncService.syncBatch(stream);
    }

    public Mono<String> submitTask() {
        SyncTask task = new SyncTask();
        task.setTaskName("user-sync");
        task.setSyncData(new SyncData("USER", payload));
        return reactiveDataSyncService.createSyncTask(task)
                .flatMap(taskId -> reactiveDataSyncService.executeSyncTask(taskId)
                        .thenReturn(taskId));
    }
}
```

### 3. Redis 分布式锁（Blocking）

```java
import cn.aryee.sync.infrastructure.blocking.lock.DistributedLock;
import cn.aryee.sync.infrastructure.blocking.lock.DistributedLockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class LockSampleService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public void doWithRedisLock(String bizKey) {
        DistributedLock lock = DistributedLockUtil.getRedisLock(stringRedisTemplate, bizKey);
        try {
            lock.lock();
            // 临界区业务逻辑
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

### 4. 本地锁兜底（无需 Redis）

```java
import cn.aryee.sync.infrastructure.blocking.lock.DistributedLockUtil;
import java.util.concurrent.TimeUnit;

public void doWithLocalLock(String bizKey) throws InterruptedException {
    DistributedLockUtil.Lock lock = DistributedLockUtil.getLocalLock(bizKey);
    if (lock.tryLock(3, TimeUnit.SECONDS)) {
        try {
            // 临界区业务逻辑
        } finally {
            lock.unlock();
        }
    }
}
```

> `DistributedLockUtil.getLock(key)` 等价于 `getLocalLock(key)`，在未配置 Redis 时使用。需要 Redis 锁时务必显式调用 `getRedisLock(redisTemplate, key)`。

## 装配条件一览

### Blocking Starter（`sync-spring-boot-starter`）

| Bean                              | 装配类                              | 生效条件                                                              |
|-----------------------------------|-------------------------------------|-----------------------------------------------------------------------|
| `dataSyncStrategyFactory`         | `AryeeSyncAutoConfiguration`        | `aryee.sync.enabled=true`（默认）                                     |
| `dataSyncService`                 | `AryeeSyncAutoConfiguration`        | 同上（默认 `DefaultDataSyncService`，依赖 `DataSyncStrategyFactory`） |
| `inMemoryDataSyncServiceFactory`  | `AryeeSyncAutoConfiguration`        | 同上（SPI 工厂兜底）                                                   |

### Reactive Starter（`sync-reactive-spring-boot-starter`）

| Bean                                    | 装配类                              | 生效条件                                                              |
|-----------------------------------------|-------------------------------------|-----------------------------------------------------------------------|
| `reactiveDataSyncService`               | `SyncReactiveAutoConfiguration`    | `aryee.sync.enabled=true`（默认）+ 类路径存在 `reactor.core.publisher.Mono` |
| `reactiveInMemoryDataSyncServiceFactory` | `SyncReactiveAutoConfiguration`   | 同上                                                                  |

> Reactive AutoConfiguration 由 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册，配置前缀仍为 `aryee.sync`（与 Blocking 共享 `SyncProperties`）。

## 模块间关系

- **依赖 commons**: `commons-core`（异常基类、常量、工具）
- **可选依赖**: `spring-boot-starter-data-redis`（Redis 分布式锁需要）、`reactor-core`（Reactive 实现需要）
- **SPI**: `META-INF/services/cn.aryee.foundation.sync.api.service.DataSyncServiceFactory` 注册默认工厂，可通过覆盖文件扩展

## 最佳实践

1. **同步任务幂等性**：同一 `SyncData` 可能因重试被多次提交，业务侧务必保证幂等（基于 `id` 或 `syncTimestamp` 去重）
2. **Redis 锁超时设置**：`RedisDistributedLock` 默认 30 秒过期 + 看门狗续期，长耗时业务请显式调大 `expireTime` 或拆分临界区
3. **锁释放**：始终在 `finally` 中调用 `unlock`，并先通过 `isHeldByCurrentThread()` 判断，避免误删他人锁
4. **响应式非阻塞**：Reactive 模式禁止在 `Mono` / `Flux` 内调用 `block()`，耗时 IO 应通过 `publishOn(Schedulers.boundedElastic())` 切换线程
5. **策略扩展**：自定义同步策略实现 `DataSyncStrategy` / `ReactiveDataSyncStrategy`，并通过 `@ConditionalOnMissingBean` 覆盖默认 `DataSyncStrategyFactory`

## 兼容性

| 环境 | 版本要求 |
|------|----------|
| JDK | 21+ |
| Spring Boot | 4.0.6 |
| Spring Data Redis | 3.x（可选，Redis 锁需要） |
| Reactor | 3.x（可选，Reactive 模式需要） |
