# Aryee Foundation 项目示例代码集合

> **所属项目**: [Aryee Foundation](../README.md)
> **类型**: 独立示例工程集合（非聚合 POM，每个示例是独立 Maven 项目）
> **技术栈**: Java 21, Spring Boot 4.0.6, Quartz, XXL-Job, OpenFeign, WebClient, Nacos
> **BOM**: 各示例通过 `dependencyManagement` 引入 `cn.aryee.foundation:bom-full` 统一版本管理

本目录包含 Aryee Foundation 各功能模块的使用示例与集成验证工程，覆盖 Blocking / Reactive 双模式，帮助开发者快速上手。同时提供三个**架构形态完整示例**，展示单体/云原生/微服务架构的完整应用搭建。

## 📁 目录结构

```
examples/
├── architecture-monolith-example/            # 单体架构完整示例（H2 嵌入式数据库，零外部依赖）
├── architecture-cloudnative-example/         # 云原生架构完整示例（K8s ConfigMap + OTel 监控）
├── architecture-microservice-example/        # 微服务架构完整示例（Nacos + Seata 分布式事务）
├── ai-examples/                              # AI 大模型示例（基础 + RAG + Web 集成）
├── ai-examples-blocking/                     # AI 模块 Blocking 模式
├── ai-examples-reactive/                    # AI 模块 Reactive 模式
├── cache-examples-blocking/                  # 缓存 Blocking 模式（Redis）
├── cache-examples-reactive/                  # 缓存 Reactive 模式（Redis）
├── database-examples-blocking/               # 数据库 Blocking 模式（JPA + MyBatis-Plus + MySQL）
├── database-examples-reactive/               # 数据库 Reactive 模式（R2DBC）
├── database-oracle-examples-blocking/        # 数据库 Oracle Blocking 模式（JDBC + Oracle）
├── database-oracle-examples-reactive/        # 数据库 Oracle Reactive 模式（R2DBC + Oracle）
├── gateway-examples-blocking/                # 网关 Blocking 模式
├── gateway-examples-reactive/               # 网关 Reactive 模式
├── messaging-examples/                       # 消息队列 Blocking 模式（RabbitMQ）
├── messaging-examples-reactive/              # 消息队列 Reactive 模式
├── messaging-kafka-examples/                # Kafka Blocking 模式
├── messaging-kafka-examples-reactive/        # Kafka Reactive 模式
├── messaging-rocketmq-examples/              # RocketMQ Blocking 模式
├── messaging-rocketmq-examples-reactive/    # RocketMQ Reactive 模式
├── scheduler-examples-blocking/              # 任务调度 Blocking 模式（Quartz / XXL-Job）
├── scheduler-examples-reactive/              # 任务调度 Reactive 模式
├── security-examples-blocking/               # 安全 Blocking 模式
├── security-examples-reactive/               # 安全 Reactive 模式
├── storage-minio-examples/                   # 对象存储 MinIO Blocking 模式
├── storage-minio-examples-reactive/          # 对象存储 MinIO Reactive 模式
├── tenant-examples-blocking/                 # 多租户 Blocking 模式
├── tenant-examples-reactive/                 # 多租户 Reactive 模式
├── transport-examples-blocking/              # 传输层 Blocking 模式（WebMVC + OpenFeign）
├── transport-examples-reactive/              # 传输层 Reactive 模式（WebFlux + WebClient）
├── monitoring-trace-examples-blocking/       # 链路追踪 Blocking 模式（TraceFilter 提取 traceId + R<T> 自动注入）含 7 个单测验证
├── monitoring-trace-examples-reactive/       # 链路追踪 Reactive 模式（TraceWebFilter）含 5 个单测验证
├── checkstyle-reactive.xml                   # Reactive 模式 Checkstyle 配置示例
└── rpc-timeout-config-example.yml            # RPC 超时配置示例
```

> examples 目录下没有聚合 `pom.xml`，每个示例都是独立可构建的 Maven 工程，独立 `groupId=cn.aryee.foundation`、`version=1.0.0-SNAPSHOT`。

## ⚠️ Examples 模块创建规范（强制规则）

### 1. 双模式分离原则（架构规则 6.1）

**支持 Blocking + Reactive 双模式的模块**，其 examples 必须拆分为两个独立的子模块：

| 模式 | 命名 | Starter 依赖 | Spring 场景 |
|---|---|---|---|
| Blocking | `{module}-examples-blocking` | `{module}-spring-boot-starter` | `spring-boot-starter-web`（Servlet/WebMVC） |
| Reactive | `{module}-examples-reactive` | `{module}-reactive-spring-boot-starter` | `spring-boot-starter-webflux`（WebFlux） |

**强制约束**：
- ❌ 禁止在同一个 examples 模块中同时引入 Blocking 和 Reactive Starter
- ❌ 禁止在同一个 examples 模块中同时引入 `spring-boot-starter-web` 和 `spring-boot-starter-webflux`
- ✅ Blocking 和 Reactive 模块必须物理隔离：独立 `pom.xml`、独立启动类、独立 `application.yml`
- ✅ 包命名隔离：`cn.aryee.examples.{module}.blocking` / `cn.aryee.examples.{module}.reactive`

### 2. 单模式模块命名

仅支持 Blocking 模式的模块（如 `messaging-examples` RabbitMQ 默认场景、`messaging-kafka-examples`、`messaging-rocketmq-examples`）可不带 `-blocking` 后缀；区分 Blocking / Reactive 时才追加后缀。

---

## 📦 示例清单与依赖关系

### 1. AI 模块示例

| 示例模块 | ArtifactId | 引入的 Starter | 第三方依赖 |
|---|---|---|---|
| `ai-examples` | `ai-examples` | `ai-spring-boot-starter` | `spring-boot-starter-web`、`spring-boot-starter-actuator` |
| `ai-examples-blocking` | `ai-examples-blocking` | `ai-spring-boot-starter` | `spring-boot-starter-web`、`spring-ai-openai-spring-boot-starter` |
| `ai-examples-reactive` | `ai-examples-reactive` | `ai-reactive-spring-boot-starter` | `spring-boot-starter-webflux` |

`ai-examples` 内的 Java 示例类：

- `cn.aryee.examples.ai.basic.LlmServiceExample` — LLM 服务使用示例（简单对话、多轮对话、流式输出）
- `cn.aryee.examples.ai.basic.EmbeddingExample` — Embedding 服务示例（文本向量化、相似度计算）
- `cn.aryee.examples.ai.rag.KnowledgeBaseExample` — 基于知识库的问答示例
- `cn.aryee.examples.ai.integration.AiWebController` — Web 应用中集成 AI 能力（REST API、SSE 流式输出）

> 详细文档见 [ai-examples/README.md](ai-examples/README.md)、[ai-examples/QUICK_START.md](ai-examples/QUICK_START.md)、[ai-examples/GUIDE.md](ai-examples/GUIDE.md)。

### 2. 数据库示例

| 示例模块 | ArtifactId | 引入的 Starter | 第三方依赖 |
|---|---|---|---|
| `database-examples-blocking` | `database-examples-blocking` | `database-spring-boot-starter` | `spring-boot-starter-data-jpa`、`mybatis-plus-spring-boot4-starter`、`mysql-connector-j`、`jsqlparser` |
| `database-examples-reactive` | `database-examples-reactive` | `database-reactive-spring-boot-starter` | `spring-boot-starter-data-r2dbc`、`spring-boot-starter-webflux` |
| `database-oracle-examples-blocking` | `database-oracle-examples-blocking` | `database-spring-boot-starter` | `spring-boot-starter-data-jpa`、`mybatis-plus-spring-boot4-starter`、`ojdbc11` |
| `database-oracle-examples-reactive` | `database-oracle-examples-reactive` | `database-reactive-spring-boot-starter` | `spring-boot-starter-data-r2dbc`、`oracle-r2dbc` |

资源文件：`application.yml`、`application-jdbc.yml`、`schema-jdbc.sql`、`schema.sql`。

### 3. 缓存示例

| 示例模块 | ArtifactId | 引入的 Starter | 第三方依赖 |
|---|---|---|---|
| `cache-examples-blocking` | `cache-examples-blocking` | `cache-spring-boot-starter` | `spring-boot-starter-data-redis`、`spring-boot-starter-web` |
| `cache-examples-reactive` | `cache-examples-reactive` | `cache-reactive-spring-boot-starter` | `spring-boot-starter-data-redis`、`spring-boot-starter-webflux`、`reactor-test` |

`cache-examples-blocking` 内的 Java 示例类：

- `cn.aryee.examples.cache.blocking.service.ProgrammaticCacheService` — **编程式缓存**：直接注入 `CacheService`，展示 get/set/delete、TTL 管理、批量 multiGet/multiSet、计数器 increment、Hash 数据结构、缓存穿透防护（空值缓存）
- `cn.aryee.examples.cache.blocking.service.DeclarativeCacheService` — **声明式缓存**：使用 Spring `@Cacheable`/`@CachePut`/`@CacheEvict` 注解，底层通过 `AryeeCacheManager` 桥接到 `CacheService`，含条件缓存（condition/unless）
- `cn.aryee.examples.cache.blocking.service.IdempotentService` — **幂等性实现**：基于 `CacheService.increment` 原子计数器实现接口幂等，支持 requestId 去重、结果缓存、TTL 窗口自动清理
- `cn.aryee.examples.cache.blocking.controller.CacheExampleController` — REST API 端点（端口 8081）

`cache-examples-reactive` 内的 Java 示例类：

- `cn.aryee.examples.cache.reactive.service.ReactiveProgrammaticCacheService` — **响应式编程式缓存**：注入 `ReactiveCacheService`，所有操作返回 `Mono`，展示 `switchIfEmpty` 缓存回源、`flatMap` 链式操作、非阻塞 TTL 管理
- `cn.aryee.examples.cache.reactive.service.ReactiveIdempotentService` — **响应式幂等性**：基于 `ReactiveCacheService.increment` 的原子计数器，全程 `Mono` 链式传递，禁止 `block()`
- `cn.aryee.examples.cache.reactive.controller.ReactiveCacheExampleController` — WebFlux REST API 端点（端口 8082）

> **快速体验**：两个示例默认使用 `aryee.cache.type=memory`（纯内存，无需 Redis），修改 `application.yml` 中 `type: redis` 可切换到 Redis 分布式缓存。

### 4. 传输层示例

| 示例模块 | ArtifactId | 引入的 Starter | 第三方依赖 |
|---|---|---|---|
| `transport-examples-blocking` | `transport-examples-blocking` | `transport-spring-boot-starter` | `spring-boot-starter-web`、`spring-cloud-starter-openfeign` |
| `transport-examples-reactive` | `transport-examples-reactive` | `transport-reactive-spring-boot-starter` | `spring-boot-starter-webflux` |

### 5. 消息队列示例

| 示例模块 | ArtifactId | 引入的 Starter | 第三方依赖（MQ） |
|---|---|---|---|
| `messaging-examples` | `messaging-examples` | `messaging-spring-boot-starter` | `spring-boot-starter-amqp`（RabbitMQ） |
| `messaging-examples-reactive` | `messaging-examples-reactive` | `messaging-reactive-spring-boot-starter` | （Reactive RabbitMQ） |
| `messaging-kafka-examples` | `messaging-kafka-examples` | `messaging-spring-boot-starter` | `spring-kafka` |
| `messaging-kafka-examples-reactive` | `messaging-kafka-examples-reactive` | `messaging-reactive-spring-boot-starter` | `spring-kafka`（Reactive） |
| `messaging-rocketmq-examples` | `messaging-rocketmq-examples` | `messaging-spring-boot-starter` | `rocketmq-spring-boot-starter` |
| `messaging-rocketmq-examples-reactive` | `messaging-rocketmq-examples-reactive` | `messaging-reactive-spring-boot-starter` | `rocketmq-spring-boot-starter`（Reactive） |

### 6. 任务调度示例

| 示例模块 | ArtifactId | 引入的 Starter | 第三方依赖 |
|---|---|---|---|
| `scheduler-examples-blocking` | `scheduler-examples-blocking` | `scheduler-spring-boot-starter` | `spring-boot-starter-web`、`spring-boot-starter-aspectj` |
| `scheduler-examples-reactive` | `scheduler-examples-reactive` | `scheduler-reactive-spring-boot-starter` | `spring-boot-starter-webflux` |

> 调度模块支持 Quartz 与 XXL-Job 两种实现，由 `aryee.scheduler.type` 选择。

### 7. 安全示例

| 示例模块 | ArtifactId | 引入的 Starter | 第三方依赖 |
|---|---|---|---|
| `security-examples-blocking` | `security-examples-blocking` | `security-spring-boot-starter` | `spring-boot-starter-web`、`spring-boot-starter-validation` |
| `security-examples-reactive` | `security-examples-reactive` | `security-reactive-spring-boot-starter` | `spring-boot-starter-webflux`、`spring-boot-starter-validation` |

### 8. 对象存储示例

| 示例模块 | ArtifactId | 引入的 Starter | 第三方依赖 |
|---|---|---|---|
| `storage-minio-examples` | `storage-minio-examples` | `storage-spring-boot-starter` | `io.minio:minio`、`spring-boot-starter-web` |
| `storage-minio-examples-reactive` | `storage-minio-examples-reactive` | `storage-reactive-spring-boot-starter` | `io.minio:minio`、`spring-boot-starter-webflux` |

### 9. 多租户示例

| 示例模块 | ArtifactId | 引入的 Starter | 第三方依赖 |
|---|---|---|---|
| `tenant-examples-blocking` | `tenant-examples-blocking` | `tenant-spring-boot-starter` | `spring-boot-starter-web`、`spring-boot-starter-aspectj`（`@TenantSwitch` 切面） |
| `tenant-examples-reactive` | `tenant-examples-reactive` | `tenant-reactive-spring-boot-starter` | `spring-boot-starter-webflux` |

### 10. 网关示例

| 示例模块 | ArtifactId | 引入的 Starter | 第三方依赖 |
|---|---|---|---|
| `gateway-examples-blocking` | `gateway-examples-blocking` | `gateway-spring-boot-starter` | `spring-boot-starter-web`、`spring-boot-starter-validation` |
| `gateway-examples-reactive` | `gateway-examples-reactive` | `gateway-reactive-spring-boot-starter` | `spring-boot-starter-webflux` |

---

## 🚀 快速开始

### 前置要求

1. **JDK 21+**（示例 `maven-compiler-plugin` 配置 `<release>21</release>`）
2. **Maven 3.9+**
3. **相应的中间件服务**（MySQL、Redis、RabbitMQ、Kafka、RocketMQ、MinIO、Nacos 等，按所选示例按需准备）
4. **Aryee Foundation 已本地安装**：先在仓库根目录执行 `mvn clean install -DskipTests` 将 `bom-full` 与各 Starter 安装到本地 Maven 仓库

### 构建与运行

每个示例都是独立的 Maven 工程，可单独构建与运行。

#### 1. 构建单个示例

```bash
# 进入示例目录
cd examples/cache-examples-blocking

# 编译并打包
mvn clean package -DskipTests
```

产物：`target/{artifactId}-1.0.0-SNAPSHOT.jar`（由 `spring-boot-maven-plugin` `repackage` 生成可执行 JAR）

#### 2. 运行示例

```bash
# 直接运行 fat jar
java -jar target/cache-examples-blocking-1.0.0-SNAPSHOT.jar

# 或通过 spring-boot-maven-plugin 运行
mvn spring-boot:run
```

#### 3. 运行集成测试

```bash
mvn test
```

Blocking 示例使用 `spring-boot-starter-test`；Reactive 示例额外引入 `reactor-test` 用于 `StepVerifier` 验证。

### 配置文件

每个示例的 `src/main/resources/application.yml` 包含该示例所需的基础配置（端口、数据源、Redis、MQ 连接等），中间件地址默认指向 `localhost`，请按实际环境调整。

---

## 📖 学习路径建议

按以下顺序学习示例：

1. **基础示例**：`cache-examples-blocking` / `database-examples-blocking` —— 单模块基础用法
2. **进阶示例**：`ai-examples` / `scheduler-examples-blocking` —— 多模块组合与高级特性
3. **Reactive 对比**：将上述 Blocking 示例与对应的 `-reactive` 示例对照学习，理解双模式差异
4. **生产实践**：`transport-examples-blocking` + `security-examples-blocking` + `tenant-examples-blocking` —— 模拟生产环境的组合使用

---

## 📂 共享资源文件

| 文件 | 说明 |
|------|------|
| `checkstyle-reactive.xml` | Reactive 模块 Checkstyle 配置示例（用于代码规范校验） |
| `rpc-timeout-config-example.yml` | RPC 超时配置示例（参考 `aryee.transport` 超时与重试策略） |

---

## 📞 问题反馈

如有问题，请通过以下方式联系：

- GitHub Issues
- 邮箱：508509000@qq.com