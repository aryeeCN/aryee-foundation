# Aryee BOM 依赖管理模块

> **所属项目**: [Aryee Foundation](../../README.md)
> **架构层次**: 版本管理（依赖版本唯一来源）
> **技术栈**: Maven 3.8+ / Java 21 / Spring Boot 4.0.6
> **Group ID**: cn.aryee.foundation

## 简介

Aryee BOM (Bill of Materials) 是整个 Aryee Foundation 项目的依赖版本管理中心，采用分层 BOM 设计，确保所有模块使用一致的依赖版本，避免版本冲突。

**核心原则**：所有依赖版本号有且只有一个定义位置 — `bom-base/pom.xml` 的 `<properties>` 和 `<dependencyManagement>`。各功能模块的 POM 中**禁止硬编码版本号**。

### 核心特性

- ✅ **版本唯一来源**: 第三方依赖版本集中在 `bom-base`，内部模块版本集中在 `bom-internal`
- ✅ **分层聚合**: 按业务领域划分子 BOM，按需引入，避免冗余依赖
- ✅ **Spring Boot 4.0.6 集成**: 完全兼容 Spring Cloud 2025.1.2 / Spring Cloud Alibaba 2025.1.0.0 生态
- ✅ **双模式支持**: 同时管理 Blocking 和 Reactive 模块的 Starter 版本
- ✅ **安全修复覆盖**: Jackson 2.22.1（CVE-2026-54512~54515）、Spring Boot 4.0.6（CVE-2026-40973）、Spring Kafka 3.3.16（CVE-2026-41726/41731）
- ✅ **CI 友好**: 配合 `flatten-maven-plugin` 支持 `${aryee.version}` 占位符

## 模块结构

BOM 模块采用分层聚合设计，共包含 23 个子模块：

```
aryee-foundation-bom/                 # BOM 聚合 POM
├── bom-base/                         # 基础依赖 BOM（所有第三方依赖版本唯一来源）
├── bom-cache/                        # 缓存模块 BOM
├── bom-database/                     # 数据库模块 BOM
├── bom-security/                     # 安全模块 BOM
├── bom-messaging/                    # 消息模块 BOM
├── bom-scheduler/                    # 调度模块 BOM
├── bom-storage/                      # 存储模块 BOM
├── bom-ai/                           # AI 模块 BOM
├── bom-doc/                          # API 文档模块 BOM
├── bom-websocket/                    # WebSocket 模块 BOM
├── bom-i18n/                         # 国际化模块 BOM
├── bom-workflow/                     # 工作流模块 BOM
├── bom-transport/                    # 传输层模块 BOM
├── bom-monitoring/                   # 监控模块 BOM
├── bom-event/                        # 事件模块 BOM
├── bom-transaction/                  # 事务模块 BOM
├── bom-sync/                         # 同步模块 BOM
├── bom-tenant/                       # 多租户模块 BOM
├── bom-gateway/                      # 网关模块 BOM
├── bom-dict/                         # 字典管理模块 BOM
├── bom-internal/                     # 内部模块版本 BOM（所有 cn.aryee.foundation 模块）
└── bom-full/                         # 完整 BOM（聚合所有子 BOM，对外发布）
```

### 子模块说明

| 模块 | 说明 | 主要内容 |
|------|------|----------|
| **bom-base** | 第三方依赖管理（版本唯一来源） | Spring Boot 4.0.6 / Spring Cloud 2025.1.2 / Spring Cloud Alibaba 2025.1.0.0 / Spring Security 7.0.5 / Spring AI 2.0.0 / Sa-Token 1.45.0 / JJWT 0.12.6 / Seata 2.5.0 / MyBatis-Plus 3.5.17 / MySQL 9.2.0 / Nacos 3.1.1 / Jackson 2.22.1 等 |
| **bom-cache** | 缓存模块管理 | cache-api / cache-infrastructure / cache-spring-boot-autoconfigure / cache-spring-boot-starter + Reactive 版本 |
| **bom-database** | 数据库模块管理 | database-api / database-infrastructure / database-spring-boot-autoconfigure / database-spring-boot-starter + Reactive 版本 |
| **bom-security** | 安全模块管理 | security-api / security-infrastructure / security-spring-boot-autoconfigure / security-spring-boot-starter + Reactive 版本 |
| **bom-messaging** | 消息模块管理 | messaging-api / messaging-infrastructure / messaging-spring-boot-autoconfigure / messaging-spring-boot-starter + Reactive 版本 |
| **bom-scheduler** | 调度模块管理 | scheduler-api / scheduler-infrastructure / scheduler-spring-boot-autoconfigure / scheduler-spring-boot-starter + Reactive 版本 |
| **bom-storage** | 存储模块管理 | storage-api / storage-infrastructure / storage-spring-boot-autoconfigure / storage-spring-boot-starter + Reactive 版本 |
| **bom-ai** | AI 模块管理 | ai-api / ai-infrastructure / ai-spring-boot-autoconfigure / ai-spring-boot-starter + Reactive 版本 |
| **bom-transport** | 传输层模块管理 | transport-api / transport-infrastructure / transport-spring-boot-autoconfigure / transport-spring-boot-starter + Reactive 版本 |
| **bom-monitoring** | 监控模块管理 | monitoring-api / monitoring-infrastructure / monitoring-spring-boot-autoconfigure / monitoring-spring-boot-starter + Reactive 版本 |
| **bom-event** | 事件模块管理 | event-api / event-infrastructure / event-spring-boot-autoconfigure / event-spring-boot-starter + Reactive 版本 |
| **bom-transaction** | 事务模块管理 | transaction-api / transaction-infrastructure / transaction-spring-boot-autoconfigure / transaction-spring-boot-starter + Reactive 版本 |
| **bom-sync** | 同步模块管理 | sync-api / sync-infrastructure / sync-spring-boot-autoconfigure / sync-spring-boot-starter + Reactive 版本 |
| **bom-tenant** | 多租户模块管理 | tenant-api / tenant-infrastructure / tenant-spring-boot-autoconfigure / tenant-spring-boot-starter + Reactive 版本 |
| **bom-gateway** | 网关模块管理 | gateway-api / gateway-infrastructure / gateway-spring-boot-autoconfigure / gateway-spring-boot-starter + Reactive 版本 |
| **bom-dict** | 字典管理模块管理 | dict-api / dict-infrastructure / dict-spring-boot-autoconfigure / dict-spring-boot-starter + Reactive 版本 |
| **bom-doc** | API 文档模块管理 | doc-spring-boot-autoconfigure / doc-spring-boot-starter + Reactive 版本 |
| **bom-websocket** | WebSocket 模块管理 | websocket-api / websocket-infrastructure / websocket-spring-boot-autoconfigure / websocket-spring-boot-starter + Reactive 版本 |
| **bom-i18n** | 国际化模块管理 | i18n-api / i18n-infrastructure / i18n-spring-boot-autoconfigure / i18n-spring-boot-starter + Reactive 版本 |
| **bom-workflow** | 工作流模块管理 | workflow-api / workflow-infrastructure / workflow-spring-boot-autoconfigure / workflow-spring-boot-starter + Reactive 版本 |
| **bom-internal** | 内部模块版本管理 | 所有 `cn.aryee.foundation` 内部模块版本锁定为 `${aryee.version}` |
| **bom-full** | 完整 BOM | 聚合所有 21 个功能模块子 BOM，对外发布 |

### BOM 层级关系

```
                    ┌──────────────────────────────────────┐
                    │           parent (技术父 POM)         │
                    │  通过 import 引入 bom-full +          │
                    │  bom-internal，统一向上传递给所有子模块 │
                    └─────────────┬────────────────────────┘
                                  │ import
                    ┌─────────────▼────────────────────────┐
                    │              bom-full                 │
                    │  聚合 21 个功能模块子 BOM              │
                    └─────────────┬────────────────────────┘
                                  │ import
        ┌─────────────┬───────────┼───────────┬─────────────┬────────────┐
        ▼             ▼           ▼           ▼             ▼            ▼
   ┌─────────┐  ┌─────────┐ ┌─────────┐ ┌─────────┐  ┌─────────┐  ┌─────────┐
   │bom-cache│  │bom-db   │ │bom-sec  │ │bom-msg  │  │bom-ai   │  │bom-trans│
   │ cache   │  │database │ │security │ │messaging│  │   ai    │  │transport│
   └────┬────┘  └────┬────┘ └────┬────┘ └────┬────┘  └────┬────┘  └────┬────┘
        │            │           │           │            │            │
        └────────────┴───────────┴───────────┴────────────┴────────────┘
                                  │ import
                    ┌─────────────▼────────────────────────┐
                    │              bom-base                 │
                    │  所有第三方依赖版本（Spring Boot /     │
                    │  Spring Cloud / AI / DB / 安全 / ...）│
                    └──────────────────────────────────────┘

                    ┌──────────────────────────────────────┐
                    │           bom-internal                │
                    │  所有内部 cn.aryee.foundation 模块     │
                    │  版本锁定为 ${aryee.version}           │
                    └──────────────────────────────────────┘
```

## 使用方法

### Maven 依赖配置

#### 方式一：使用完整 BOM（推荐）

在项目的父 `pom.xml` 中引入 `bom-full`，即可管理所有依赖版本：

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

#### 方式二：按需引入子 BOM

根据项目需要，只引入特定领域的 BOM（每个子 BOM 都会自动 import `bom-base`）：

```xml
<dependencyManagement>
    <dependencies>
        <!-- 传输层模块 -->
        <dependency>
            <groupId>cn.aryee.foundation</groupId>
            <artifactId>bom-transport</artifactId>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- 数据库模块 -->
        <dependency>
            <groupId>cn.aryee.foundation</groupId>
            <artifactId>bom-database</artifactId>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- 缓存模块 -->
        <dependency>
            <groupId>cn.aryee.foundation</groupId>
            <artifactId>bom-cache</artifactId>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

#### 方式三：使用父 POM 继承

让项目的父 POM 直接继承 Aryee 技术父 POM `parent`（自动获得 BOM + 插件配置）：

```xml
<parent>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>aryee-foundation-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>
```

> 注意：外部项目应继承 `parent`（技术父 POM），而非 BOM 内部模块。

### 使用依赖

引入 BOM 后，在项目中添加依赖时**无需指定版本号**（版本由 BOM 统一管理）：

```xml
<dependencies>
    <!-- 公共基础 -->
    <dependency>
        <groupId>cn.aryee.foundation</groupId>
        <artifactId>commons-core</artifactId>
    </dependency>

    <!-- 数据库模块（Blocking） -->
    <dependency>
        <groupId>cn.aryee.foundation</groupId>
        <artifactId>database-spring-boot-starter</artifactId>
    </dependency>

    <!-- 缓存模块（Reactive） -->
    <dependency>
        <groupId>cn.aryee.foundation</groupId>
        <artifactId>cache-reactive-spring-boot-starter</artifactId>
    </dependency>

    <!-- 安全模块 -->
    <dependency>
        <groupId>cn.aryee.foundation</groupId>
        <artifactId>security-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

## 第三方依赖版本清单（bom-base）

以下为 `bom-base/pom.xml` 中实际声明的关键版本（节选）：

| 分类 | 依赖 | 版本 |
|---|---|---|
| **Spring 生态** | spring-boot | 4.0.6 |
| | spring-cloud | 2025.1.2 |
| | spring-cloud-alibaba | 2025.1.0.0 |
| | spring-security-bom | 7.0.5 |
| | spring-ai-bom | 2.0.0 |
| | spring-session | 3.5.6 |
| **JSON** | jackson | 2.22.1（覆盖 Spring Boot 默认，修复 CVE-2026-54512~54515） |
| | jackson-annotations | 2.22 |
| | fastjson2 | 2.0.55 |
| **持久化** | mybatis-plus | 3.5.17 |
| | mysql-connector | 9.2.0 |
| | r2dbc-mysql | 1.1.5.RELEASE |
| | oracle-jdbc | 23.5.0.24.07 |
| | jsqlparser | 4.9 |
| **安全** | sa-token | 1.45.0 |
| | jjwt | 0.12.6 |
| | keycloak | 25.0.6 |
| | nimbus-jose-jwt | 9.44 |
| **分布式** | seata (org.apache.seata) | 2.5.0 |
| | nacos-client | 3.1.1 |
| | resilience4j | 2.3.0 |
| **消息队列** | rocketmq | 5.3.1 |
| | spring-kafka | 3.3.16（覆盖 Spring Boot 默认，修复 CVE-2026-41726/41731） |
| **调度** | quartz | 2.3.2 |
| | xxl-job | 2.5.1 |
| **存储 SDK** | minio | 8.5.15 |
| | aliyun-sdk-oss | 3.18.5 |
| | qcloud-cos | 5.6.220 |
| | qiniu | 7.17.0 |
| | aws-java-sdk-s3 | 1.12.765 |
| **AI/向量** | dashscope-sdk-java | 2.16.1 |
| | milvus-sdk-java | 2.5.1 |
| | langchain4j | 0.36.2 |
| | tiktoken-java | 1.1.3 |
| | redisson | 3.38.1 |
| **CLI/脚手架** | picocli | 4.7.6 |
| | kubernetes-client (fabric8) | 6.13.4 |
| | freemarker | 2.3.33 |
| | snakeyaml | 2.3 |
| **API 文档** | springdoc-openapi-starter | 2.8.6 |
| **监控** | micrometer-tracing | 1.5.1 |
| | opentelemetry | 1.48.0 |
| | zipkin-reporter-brave | 3.4.2 |
| | skywalking | 9.2.0 |
| **测试** | junit-jupiter | 5.11.3 |
| | mockito | 5.15.2 |
| | assertj | 3.26.3 |
| | testcontainers | 1.20.4 |
| | reactor-test | 3.7.2 |
| **代码质量** | lombok | 1.18.42 |
| | jacoco | 0.8.12 |

## 最佳实践

### 1. BOM 选择指南

**使用 bom-full 的场景**：
- ✅ 新项目快速启动
- ✅ 项目功能全面，需要多个领域模块
- ✅ 统一管理所有依赖版本

**按需引入子 BOM 的场景**：
- ✅ 项目功能单一，只需要特定模块
- ✅ 减少依赖管理复杂度
- ✅ 对构建速度有严格要求

### 2. 版本管理规范

```xml
<!-- ✅ 正确：通过 import BOM 统一管理，依赖声明不写 version -->
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

<dependencies>
    <dependency>
        <groupId>cn.aryee.foundation</groupId>
        <artifactId>cache-spring-boot-starter</artifactId>
        <!-- 不指定 version，由 BOM 管理 -->
    </dependency>
</dependencies>
```

```xml
<!-- ❌ 错误：内部模块禁止硬编码 version -->
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>commons-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>  <!-- 禁止！版本由 BOM/bom-internal 锁定 -->
</dependency>
```

### 3. 覆盖特定依赖版本

当需要覆盖 BOM 中的版本时，在自己项目的 `dependencyManagement` 中重新声明（**必须放在 BOM import 之后**才会生效）：

```xml
<dependencyManagement>
    <dependencies>
        <!-- Aryee BOM（先 import） -->
        <dependency>
            <groupId>cn.aryee.foundation</groupId>
            <artifactId>bom-full</artifactId>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- 覆盖特定依赖版本（后声明优先） -->
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2</artifactId>
            <version>2.0.55</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 4. 升级依赖版本

**仅修改 `bom-base/pom.xml` 中对应的 `{xxx}.version` property 一处即可全项目生效**：

```xml
<!-- bom-base/pom.xml -->
<properties>
    <spring.boot.version>4.0.6</spring.boot.version>  <!-- 升级只改这里 -->
</properties>
```

修改后执行 `mvn validate` + 编译确认无冲突。

### 5. 多模块项目配置

对于多模块项目，建议在根 POM 中统一配置 BOM：

```xml
<!-- 根 pom.xml -->
<groupId>com.example</groupId>
<artifactId>my-project-parent</artifactId>
<packaging>pom</packaging>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>cn.aryee.foundation</groupId>
            <artifactId>bom-full</artifactId>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- 项目内部模块版本管理 -->
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>my-project-common</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 6. 响应式项目配置

如果项目使用响应式编程模型，注意选择响应式版本的 Starter（`-reactive-spring-boot-starter`）：

```xml
<dependencies>
    <!-- 响应式数据库 -->
    <dependency>
        <groupId>cn.aryee.foundation</groupId>
        <artifactId>database-reactive-spring-boot-starter</artifactId>
    </dependency>

    <!-- 响应式 Redis 缓存 -->
    <dependency>
        <groupId>cn.aryee.foundation</groupId>
        <artifactId>cache-reactive-spring-boot-starter</artifactId>
    </dependency>

    <!-- 响应式传输 -->
    <dependency>
        <groupId>cn.aryee.foundation</groupId>
        <artifactId>transport-reactive-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

> ⚠️ Blocking 与 Reactive Starter **禁止同时引入**，必须二选一。

## 兼容性

### JDK 版本

| JDK 版本 | 兼容性 | 说明 |
|---------|--------|------|
| Java 21 | ✅ 完全支持 | 推荐版本（LTS），框架目标版本 |
| Java 17 | ⚠️ 部分支持 | 可能存在兼容性问题，不推荐 |
| Java 11 及以下 | ❌ 不支持 | 需要 Jakarta EE 9+ 支持 |

### Spring Boot 版本

| Spring Boot 版本 | 兼容性 | 说明 |
|-----------------|--------|------|
| 4.0.x | ✅ 完全支持 | 当前版本 4.0.6（Spring Framework 7 / Jakarta EE 11 / Jackson 3） |
| 3.5.x 及以下 | ❌ 不支持 | 框架已升级到 Boot 4，历史版本请使用旧版框架 |
| 2.x | ❌ 不支持 | 需要 Jakarta EE 迁移 |

### Spring Cloud 版本

| Spring Cloud 版本 | Spring Boot 版本 | 兼容性 |
|-------------------|-----------------|--------|
| 2025.1.x（Oakwood） | 4.0.x | ✅ 完全支持 |
| 2025.0.x | 3.5.x | ❌ 不再支持（历史版本） |
| 2023.0.x | 3.2.x ~ 3.4.x | ❌ 不再支持（历史版本） |

### Spring Cloud Alibaba 版本

| SCA 版本 | 锁定的组件 | 说明 |
|---|---|---|
| 2025.1.0.0 | Nacos 3.1.1 / Seata 2.5.0 / Sentinel 1.8.9 / RocketMQ 5.3.1 | 与 Spring Cloud 2025.1.2 + Spring Boot 4.0.x 配套 |

### 构建工具

| 工具 | 版本要求 | 说明 |
|------|---------|------|
| Maven | 3.8+ | 推荐 3.9+ |
| Gradle | 7.x+ | 需要自行适配（不推荐，未做官方支持） |

---

**作者**: Aryee Foundation Team
**版本**: 1.0.0-SNAPSHOT
