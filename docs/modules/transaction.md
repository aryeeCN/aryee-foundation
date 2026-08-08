# Aryee Transaction 分布式事务基础设施模块

> **所属项目**: [Aryee Foundation](../../README.md)
> **架构层次**: 基础设施层 (Foundation Layer)
> **技术栈**: Java 21, Spring Boot 4.0.7, Apache Seata 2.5.0 (`org.apache.seata`), AspectJ, OpenFeign

## 简介

分布式事务基础设施模块，提供基于 **Apache Seata** 的全局事务便利封装。

### 核心特性

- ✅ **Seata 便利封装（默认）**：通过 `aryee.transaction.type=seata` 装配，封装 Seata 全局事务的开始 / 提交 / 回滚 / XID 查询
- ✅ **声明式全局事务**：`@AryeeGlobalTransactional` 注解 + AOP 拦截，委托 Seata `TransactionalTemplate` 完成编排
- ✅ **Feign XID 透传**：`SeataXidFeignInterceptor` 自动将 XID 写入 Feign 请求头，跨服务延续全局事务上下文
- ✅ **事务钩子**：`SeataTransactionHook` 在事务生命周期节点输出监控日志，可对接 monitoring 模块

> **重要**：本模块依赖的是 **Apache Seata 2.5.0**（`org.apache.seata` 包），不是旧版 `io.seata`。版本由 `aryee-foundation-bom/bom-base` 的 `seata.version=2.5.0` 统一管理。

## 架构定位

```
aryee-foundation-transaction/                ← 聚合 POM（packaging=pom）
├── transaction-api/                         ← 契约层
│   ├── annotation/    AryeeGlobalTransactional, TransactionMeta
│   ├── config/         TransactionProperties (prefix = aryee.transaction)
│   ├── constant/       TransactionConstants
│   ├── exception/      TransactionException
│   ├── model/          TransactionIsolationEnum
│   └── service/        GlobalTransactionService
│
├── transaction-infrastructure/             ← 实现层
│   └── blocking/
│       └── seata/                           ← Seata 便利封装（默认）
│           ├── SeataGlobalTransactionService    GlobalTransactionService 实现
│           ├── AryeeGlobalTransactionInterceptor  @AryeeGlobalTransactional AOP 拦截
│           ├── SeataXidFeignInterceptor          Feign XID 透传
│           └── SeataTransactionHook              事务钩子（注册到 TransactionHookManager）
│
├── transaction-spring-boot-autoconfigure/   ← Blocking 自动装配
│   └── SeataAutoConfiguration
│       （META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports 注册）
│
├── transaction-spring-boot-starter/         ← Blocking Starter 依赖聚合
│
├── transaction-reactive-spring-boot-autoconfigure/  ← Reactive 自动装配（空壳，无 Bean）
│
└── transaction-reactive-spring-boot-starter/  ← Reactive Starter 依赖聚合
```

- **依赖方向**: Starter → Autoconfigure → Infrastructure → API
- **BOM 管理**: Seata 版本由 `aryee-foundation-bom/bom-base` 统一管理（`seata.version=2.5.0`）
- **双模式隔离**（参见 architecture.md §6.1）：Blocking 与 Reactive 使用独立的 Starter / Autoconfigure 模块，用户二选一引入，禁止同时引入

## Maven 依赖

### Blocking 模式（默认，Servlet/WebMVC 场景）

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>transaction-spring-boot-starter</artifactId>
    <!-- 版本由 bom-full 统一管理，无需指定 -->
</dependency>
```

### Reactive 模式（WebFlux 场景）

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>transaction-reactive-spring-boot-starter</artifactId>
</dependency>
```

> - 模块内部将 `seata-spring-boot-starter`、`aspectjweaver`、`feign-core` 标记为 `optional`，由使用方微服务工程按需引入 Seata / OpenFeign 的实际实现，避免强制依赖污染。
> - **Reactive Starter 当前无自动装配 Bean**（自研 Saga/TCC reactive 实现已移除）。Seata 全局事务主要面向 Blocking 场景。Reactive 应用建议使用 R2DBC 本地事务 + 消息驱动最终一致性（event 模块 MQ 背板）。

## 配置说明

配置前缀：`aryee.transaction`

```yaml
aryee:
  transaction:
    # 是否启用事务模块，默认 true
    enabled: true
    # 事务实现类型：seata（默认，Apache Seata 全局事务便利封装）
    type: seata
    # 默认事务超时（毫秒）
    default-timeout: 30000
    # 是否开启统计
    enable-statistics: false
```

Seata 自身的注册中心 / 配置中心 / 数据源代理等参数仍遵循 `seata.*` 原生配置，本模块不重复定义。

## 使用示例

### 1. 声明式全局事务（推荐）

在需要开启全局事务的方法上标注 `@AryeeGlobalTransactional`，由 AOP 拦截器自动 begin/commit/rollback：

```java
import cn.aryee.transaction.api.annotation.AryeeGlobalTransactional;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    /**
     * 创建订单：跨订单 / 库存 / 账户三个服务的全局事务
     */
    @AryeeGlobalTransactional(name = "createOrder", timeout = 60000, rollbackFor = {BusinessException.class})
    public Order createOrder(CreateOrderRequest request) {
        Order order = orderFacade.create(request);                  // 远程: 订单服务
        inventoryFacade.deduct(order.getItems());                   // 远程: 库存服务
        accountFacade.deduct(order.getUserId(), order.getAmount()); // 远程: 账户服务
        return order;
    }
}
```

- 任一远程调用抛出 `rollbackFor` 内异常时，Seata TM 自动发起全局回滚
- `name` 用于监控 / 日志识别；留空时使用 `类名.方法名`
- `noRollbackFor` 优先级高于 `rollbackFor`，可排除特定异常不回滚

### 2. 编程式全局事务

通过注入 `GlobalTransactionService` 手动控制全局事务生命周期：

```java
import cn.aryee.transaction.api.exception.TransactionException;
import cn.aryee.transaction.api.service.GlobalTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ManualTransactionService {

    @Autowired
    private GlobalTransactionService globalTransactionService;

    public void doInGlobalTx() {
        String xid = null;
        try {
            xid = globalTransactionService.begin("manualTx", 30000);
            // ... 执行业务（含远程调用，XID 已通过 Seata RootContext 绑定当前线程）
            globalTransactionService.commit(xid);
        } catch (Exception e) {
            if (xid != null) {
                try {
                    globalTransactionService.rollback(xid);
                } catch (TransactionException ex) {
                    // 记录回滚失败日志，必要时告警人工介入
                }
            }
            throw e;
        }
    }

    public boolean inTransaction() {
        return globalTransactionService.isActive();
    }

    public String currentXid() {
        return globalTransactionService.getCurrentXid();
    }
}
```

### 3. Feign 跨服务 XID 透传

`SeataXidFeignInterceptor` 在装配 `type=seata` 且类路径存在 Feign 时自动注册。无需额外配置，发起 Feign 调用时当前线程的 XID 会自动写入请求头 `RootContext.KEY_XID`，下游服务解析后即可加入同一全局事务。

> 仅需保证下游服务同样引入了 Seata 客户端与本 Starter。

## 装配条件一览

### Blocking Starter（`transaction-spring-boot-starter`）

| Bean                              | 装配类                              | 生效条件                                              |
|-----------------------------------|-------------------------------------|-------------------------------------------------------|
| `seataGlobalTransactionService`   | `SeataAutoConfiguration`            | `type=seata`（默认）+ 类路径存在 `org.apache.seata.spring.annotation.GlobalTransactional` |
| `aryeeGlobalTransactionInterceptor` | `SeataAutoConfiguration`          | 同上                                                  |
| `seataXidFeignInterceptor`        | `SeataAutoConfiguration`            | 同上 + 类路径存在 `feign.RequestInterceptor`          |

### Reactive Starter（`transaction-reactive-spring-boot-starter`）

当前无自动装配 Bean（自研 Saga/TCC reactive 实现已于 2026-08-04 移除）。Reactive 应用建议使用 R2DBC 本地事务 + 消息驱动最终一致性。

## 模块间关系

- **依赖**: `commons-core`（异常基类 `GlobalException`、`SystemException`）、`transaction-api`、`transaction-infrastructure`
- **可选依赖**: `seata-spring-boot-starter`（org.apache.seata 2.5.0）、`aspectjweaver`、`feign-core`（均 `optional`）
- **Reactive 版本**: 由 `transaction-reactive-spring-boot-starter` 提供，当前无自动装配 Bean。Seata 全局事务暂不提供 Reactive 版本，WebFlux 应用建议使用 R2DBC 本地事务 + 消息驱动最终一致性

## 最佳实践

1. **超时设置**：`@AryeeGlobalTransactional` 的 `timeout` 应略大于业务最慢分支的执行时间，避免全局事务被 TC 超时回滚后业务仍在提交本地事务
2. **回滚策略**：业务自定义异常务必加入 `rollbackFor`，Seata 默认仅对 `RuntimeException` 回滚
3. **幂等性**：全局事务重试 / 补偿场景下，分支事务必须保证幂等
4. **避免长事务**：全局事务中不要包含耗时外部调用（如大文件上传、长轮询），否则会大幅降低吞吐
