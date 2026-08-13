# Aryee Foundation

<div align="center">

![Aryee Foundation](https://img.shields.io/badge/Aryee-Foundation-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.7-green)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.1.2-blue)
![License](https://img.shields.io/badge/License-Apache%202.0-blue)

企业级 Java 全架构开发框架 — 单体 / 微服务 / 云原生，一行依赖切换架构

[快速开始](docs/getting-started.md) · [文档中心](docs/README.md) · [架构设计](#架构设计) · [模块文档](docs/README.md#模块文档) · [示例代码](examples/README.md)

</div>

## 项目简介

Aryee Foundation 是一个**企业级 Java 全架构开发框架**，旨在为**所有架构形态**（单体 / 微服务 / 云原生）提供开箱即用的完整解决方案。框架基于 Spring Boot 4.0.7 和 Spring Cloud 2025.1.2 构建，采用 DDD 领域驱动设计理念，支持 Blocking 和 Reactive 双编程模型。

> 📦 **获取方式**：框架以二进制构件形式发布到 **Maven Central**（groupId `cn.aryee.foundation`），
> 本仓库为官方**文档与示例仓库**。引入方式见 [快速开始](docs/getting-started.md)。

### 核心特性

- ✅ **全架构覆盖**: 三大架构形态 Starter（monolith/cloudnative/microservice），一行依赖切换，业务代码零变更
- ✅ **统一架构**: 标准化的四层架构设计（API 契约层 → Infrastructure 实现层 → Autoconfigure 装配层 → Starter 聚合层）
- ✅ **双编程模型**: 同时支持 Blocking 和 Reactive 两套实现，满足不同场景需求
- ✅ **全栈基础能力**: 提供缓存/数据库/存储/安全/消息/事件/调度/多租户/分布式锁/监控/AI 等 20+ 通用基础能力
- ✅ **开箱即用**: 提供完整的 Starter 依赖，简化集成配置
- ✅ **生产就绪**: 内置监控、链路追踪、告警渠道、异常处理等生产级特性
- ✅ **高度可扩展**: 模块化设计，支持按需引入和定制化开发

### 技术栈

| 分类 | 组件 | 版本 |
|---|---|---|
| **JDK** | Java | 21 |
| **Spring 生态** | Spring Boot | 4.0.7 |
| | Spring Cloud | 2025.1.2 |
| | Spring Cloud Alibaba | 2025.1.0.0 |
| | Spring Security | 7.0.6 |
| | Spring AI | 2.0.0 |
| | Spring Session | 4.0.4 |
| **缓存** | Caffeine | — |
| | Redisson | 3.38.1 |
| **数据库** | MyBatis-Plus | 3.5.17 |
| | MySQL Connector | 9.2.0 |
| | R2DBC MySQL | 1.1.5.RELEASE |
| **安全** | Sa-Token | 1.45.0 |
| | JJWT | 0.12.6 |
| **分布式事务** | Seata (org.apache.seata) | 2.5.0 |
| **配置中心** | Nacos | 3.1.1 |
| **消息队列** | RocketMQ | 5.3.1 |
| | Spring Kafka | 3.3.16 |
| **调度** | Quartz | 2.3.2 |
| | XXL-Job | 3.4.2 |
| **存储** | MinIO | 8.5.15 |
| | Aliyun OSS SDK | 3.18.5 |
| | Qcloud COS | 5.6.220 |
| | Qiniu | 7.17.0 |
| **构建工具** | Maven | 3.8+ |

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.8+
- IDE: IntelliJ IDEA (推荐)

### 引入依赖

**Step 1**：引入 `bom-full` 统一管理所有模块版本：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>cn.aryee.foundation</groupId>
            <artifactId>bom-full</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

> 当前阶段发布的是 **SNAPSHOT 版本**（可随时更新），消费方需要额外配置快照仓库，
> 详见 [快速开始](docs/getting-started.md#配置快照仓库)。正式版发布后将切换为稳定版本号。

**Step 2**：按需引入所需模块 Starter（版本由 BOM 统一管理，无需写 `<version>`）：

```xml
<!-- 缓存模块 -->
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>cache-spring-boot-starter</artifactId>
</dependency>

<!-- 数据库模块 -->
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>database-spring-boot-starter</artifactId>
</dependency>

<!-- 安全模块 -->
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>security-spring-boot-starter</artifactId>
</dependency>

<!-- Reactive 模式：替换为 *-reactive-spring-boot-starter 即可 -->
<!-- <artifactId>cache-reactive-spring-boot-starter</artifactId> -->
```

> 不同架构形态的推荐模块组合，参见 [examples/architecture-monolith-example](examples/architecture-monolith-example)、
> [examples/architecture-microservice-example](examples/architecture-microservice-example)、
> [examples/architecture-cloudnative-example](examples/architecture-cloudnative-example)。

### 配置示例

```yaml
aryee:
  cache:
    enabled: true
    type: redis
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
```

### 代码示例

```java
@Service
public class UserService {

    private final CacheService cacheService;

    public UserService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    public User getUser(String userId) {
        // Blocking 模式
        User user = cacheService.get("user:" + userId, User.class);
        if (user == null) {
            user = userRepository.findById(userId);
            cacheService.put("user:" + userId, user);
        }
        return user;
    }
}
```

> Reactive 模式请使用 `ReactiveCacheService` 接口（返回 `Mono<T>` / `Flux<T>`），与 Blocking 接口方法一一对应。

完整上手教程见 **[docs/getting-started.md](docs/getting-started.md)**。

## 架构设计

### 模块全景

```
cn.aryee.foundation                    # Maven Central groupId
├── aryee-foundation-bom               # 依赖版本管理（bom-base/bom-internal/bom-full + 按模块 BOM）
├── aryee-foundation-commons           # 公共基础模块
│   ├── commons-core                   # 纯 JDK 核心（异常/响应/分页/ID/加密/工具）
│   ├── commons-domain                 # 通用领域模型（BaseEntity/DTOModel/VOModel/PageResult）
│   ├── commons-spring                 # Spring 环境工具（BeanConvert/Json/SpringUtil/AopUtil）
│   ├── commons-servlet                # Servlet/MVC 专属（过滤器/异常处理/CORS/日志切面）
│   └── commons-excel                  # Excel 导入导出工具（基于 EasyExcel）
├── aryee-foundation-cache             # 缓存模块（Redis + Caffeine）
├── aryee-foundation-database          # 数据库模块（MyBatis-Plus + JPA + R2DBC + H2 嵌入式默认）
├── aryee-foundation-security          # 安全模块（本地 JWT 核心 + Sa-Token/Keycloak/OAuth 可插拔适配器）
├── aryee-foundation-storage           # 存储模块（Local/OSS/COS/Qiniu/MinIO）
├── aryee-foundation-messaging         # 消息模块（Kafka/RabbitMQ/RocketMQ，收发完整）
├── aryee-foundation-transport         # 传输模块（入站 Web + 出站 OpenFeign/WebClient，多注册中心 SPI）
├── aryee-foundation-monitoring        # 监控模块（Micrometer + OpenTelemetry + 告警渠道）
├── aryee-foundation-scheduler         # 调度模块（Quartz + XXL-Job）
├── aryee-foundation-event             # 事件模块（InMemory + Kafka/RabbitMQ/RocketMQ 背板）
├── aryee-foundation-transaction       # 事务模块（Local 默认 + Seata 可选，统一 @AryeeTransactional 门面）
├── aryee-foundation-sync              # 同步模块（分布式锁 + 数据同步）
├── aryee-foundation-tenant            # 多租户模块（JDBC + R2DBC 隔离）
├── aryee-foundation-ai                # AI 模块（LLM + RAG + Agent + Embedding + VectorStore）
├── aryee-foundation-gateway           # 网关增强模块（限流/熔断/灰度/聚合，MVC + Reactive 双栈）
├── aryee-foundation-apidoc               # API 文档模块（OpenAPI 分组 + 安全方案声明）
├── aryee-foundation-dict              # 字典管理模块
├── aryee-foundation-websocket         # WebSocket 实时通信模块
├── aryee-foundation-i18n              # 国际化模块（5 种 Locale 解析策略）
├── aryee-foundation-workflow          # 工作流模块（Flowable 集成）
└── aryee-foundation-cli               # 脚手架工具（项目初始化 + 代码生成）
```

### 四层架构

每个功能模块采用标准的四层架构设计（API 契约层 → Infrastructure 实现层 → Autoconfigure 装配层 → Starter 聚合层）：

```
aryee-foundation-{module}/
├── {module}-api/                              # API 层（契约层，禁止业务逻辑）
│   ├── annotation/                            # 注解定义
│   ├── config/                                # 配置属性（@ConfigurationProperties）
│   ├── enums/                                 # 枚举定义
│   ├── exception/                             # 异常定义（继承 GlobalException）
│   ├── model/                                 # 数据模型/领域模型
│   └── service/                               # 服务接口（Blocking + Reactive 契约）
├── {module}-infrastructure/                   # 基础设施层（技术实现）
│   ├── blocking/{impl-type}/                  # Blocking 实现（按实现类型分子包）
│   └── reactive/{impl-type}/                  # Reactive 实现
├── {module}-spring-boot-autoconfigure/        # 自动配置层
│   └── Aryee{Module}AutoConfiguration.java    # 条件装配 + @EnableConfigurationProperties
├── {module}-spring-boot-starter/              # Starter 层（Blocking，依赖聚合，零代码）
├── {module}-reactive-spring-boot-autoconfigure/  # Reactive 自动配置层
└── {module}-reactive-spring-boot-starter/     # Reactive Starter 层
```

> 依赖方向：`Starter → Autoconfigure → Infrastructure → API`。禁止反向依赖。
> 使用方通常只需引入 Starter，其余层构件由依赖传递自动引入。

### 架构形态选择

框架通过 **`bom-full` + 架构示例项目** 覆盖三大架构场景，复制对应示例的依赖组合即可开箱即用：

| 架构形态 | 示例项目 | 聚合能力 | 适用场景 |
|---------|---------|---------|---------|
| **单体架构** | `architecture-monolith-example` | commons-servlet + commons-excel + cache(Caffeine) + database(H2 嵌入式) + storage(Local) + event(InMemory) + scheduler(Quartz) + security + transport(Static) + transaction(Local) | 小型应用、演示、单体零外部依赖 |
| **微服务架构** | `architecture-microservice-example` | cache(Redis) + database(MySQL) + transport(Nacos) + gateway(Reactive) + transaction(Seata) + scheduler(XXL-Job) + security + monitoring | 微服务集群，Nacos 注册发现 + Seata 分布式事务 |
| **云原生架构** | `architecture-cloudnative-example` | transport(K8s) + gateway(mesh/Istio) + monitoring(OTel) + security + storage + event + scheduler | K8s 云原生环境，ConfigMap 热刷新 |

> 详细架构选型指南见 [架构选型指南](docs/guides/ARCHITECTURE_SELECTION_GUIDE.md)，迁移方案见 [跨架构迁移手册](docs/guides/MIGRATION_GUIDE.md)。

### 双编程模型支持

框架同时支持 Blocking 和 Reactive 两套编程模型：

- **Blocking 模式**: 适用于传统 Servlet Web 应用、简单 CRUD、团队熟悉阻塞式编程
- **Reactive 模式**: 适用于 WebFlux 响应式应用、高并发低延迟场景、充分利用异步 IO 优势

**安全隔离原则**：Blocking 与 Reactive 接口、实现、Starter 完全分离，用户必须二选一引入，禁止同时引入。

## 模块文档

完整模块文档索引见 **[docs/README.md](docs/README.md)**，常用入口：

| 模块 | 说明 | Blocking Starter | Reactive Starter |
|------|------|------------------|------------------|
| [commons](docs/modules/commons.md) | 公共基础模块（异常/响应/分页/ID/加密/工具/Bean 转换/JSON/Web 工具） | —（基础依赖） | —（基础依赖） |
| [cache](docs/modules/cache.md) | 缓存模块（Redis + Caffeine） | cache-spring-boot-starter | cache-reactive-spring-boot-starter |
| [database](docs/modules/database.md) | 数据库模块（MyBatis-Plus + JPA + R2DBC） | database-spring-boot-starter | database-reactive-spring-boot-starter |
| [security](docs/modules/security.md) | 安全模块（本地 JWT 核心 + Sa-Token/Keycloak/OAuth 适配器） | security-spring-boot-starter | security-reactive-spring-boot-starter |
| [storage](docs/modules/storage.md) | 存储模块（Local/OSS/COS/Qiniu/MinIO） | storage-spring-boot-starter | storage-reactive-spring-boot-starter |
| [messaging](docs/modules/messaging.md) | 消息模块（Kafka/RabbitMQ/RocketMQ，收发完整） | messaging-spring-boot-starter | messaging-reactive-spring-boot-starter |
| [ai](docs/modules/ai.md) | AI 模块（LLM + RAG + Agent + Embedding + VectorStore） | ai-spring-boot-starter | ai-reactive-spring-boot-starter |
| [event](docs/modules/event.md) | 事件模块（InMemory + Kafka/RabbitMQ/RocketMQ 背板） | event-spring-boot-starter | event-reactive-spring-boot-starter |
| [scheduler](docs/modules/scheduler.md) | 调度模块（Quartz + XXL-Job） | scheduler-spring-boot-starter | scheduler-reactive-spring-boot-starter |
| [tenant](docs/modules/tenant.md) | 多租户模块（JDBC + R2DBC 隔离） | tenant-spring-boot-starter | tenant-reactive-spring-boot-starter |

> 其余模块（transport/monitoring/transaction/sync/gateway/doc/dict/websocket/i18n/workflow/bom/cli）见 [文档中心](docs/README.md)。

## 最佳实践

### 1. 模块选择原则

- **Blocking 模式**: 传统 Web 应用、简单 CRUD、团队熟悉阻塞编程
- **Reactive 模式**: 高并发场景、WebFlux 应用、需要充分利用异步 IO

### 2. 依赖管理

使用 BOM 统一管理依赖版本，子模块依赖声明**省略 `<version>` 标签**：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>cn.aryee.foundation</groupId>
            <artifactId>bom-full</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 3. 异常处理

使用统一的异常处理机制（`GlobalException` 为框架唯一基础异常根类）：

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GlobalException.class)
    public R<Void> handleGlobalException(GlobalException ex) {
        return R.fail(ex.getCode(), ex.getMessage());
    }
}
```

**异常体系规范**：
- `GlobalException` → `BusinessException`（业务校验）/ `SystemException`（系统故障）/ `AuthException`（认证）/ `AuthorizationException`（权限）/ `ResourceNotFoundException`（资源不存在）/ `ServiceException`（服务调用）
- 业务代码抛异常统一使用 `GlobalException.throwError()` 或其子类，禁止直接 `throw new RuntimeException()`
- `catch` 块禁止静默吞噬异常，必须至少记录日志

### 4. 统一响应协议

使用 `R<T>` 作为对外响应格式，分页查询通过 `addExtra()` 携带扩展字段：

```java
public R<List<UserVO>> pageUsers(UserQuery query) {
    PageResult<UserVO> page = userService.page(query);
    return R.ok(page.getRecords())
            .addExtra("total", page.getTotal())
            .addExtra("pageNum", page.getPageNum())
            .addExtra("pageSize", page.getPageSize());
}
```

## 版本与路线图

### 1.0.0-SNAPSHOT（当前阶段）

- ✅ 完整四层架构（API/Infrastructure/Autoconfigure/Starter）+ Blocking/Reactive 双模式
- ✅ 20+ 功能模块 + commons + bom + cli，已发布至 Maven Central 快照仓
- ✅ Spring Boot 4.0.7 / Spring Framework 7 / Jakarta EE 11 / Jackson 3 / Spring Cloud 2025.1.2
- ✅ 集成 Spring AI 2.0.0（LLM/Embedding/VectorStore/RAG/Agent/Session/Prompt）
- ✅ 三大架构形态完整示例（monolith/cloudnative/microservice）
- ✅ BOM 体系完善：21 个功能模块 BOM + bom-internal + bom-full，统一版本管理

> **计划**：SNAPSHOT 阶段持续迭代修复，API 稳定后发布 **1.0.0 正式版**。

## 反馈与支持

- **问题反馈**: [GitHub Issues](https://github.com/aryeecn/aryee-foundation/issues)
- **技术支持**: 508509000@qq.com
- **文档勘误**: 欢迎提交文档相关的 Pull Request（本仓库仅包含文档与示例代码）

## 许可证

本项目采用 Apache License 2.0 许可证 - 详见 [LICENSE](LICENSE) 文件

---

<div align="center">

**[⬆ 回到顶部](#aryee-foundation)**

Made with ❤️ by Aryee Foundation Team

</div>
