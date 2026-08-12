# Aryee Foundation 架构选型指南

> **版本**: 1.2.0+
> **更新日期**: 2026-08-05
> **所属项目**: [Aryee Foundation](../README.md)

## 概述

Aryee Foundation 通过 **`bom-full` 统一版本管理 + 架构示例项目** 覆盖从单体到云原生的全架构场景。开发者复制对应示例的依赖组合即可获得完整开箱即用体验，且**业务代码零变更**。

---

## 三形架构总览

| 特性 | 单体架构 | 云原生架构 | 微服务架构 |
|------|---------|-----------|-----------|
| **示例项目** | `architecture-monolith-example` | `architecture-cloudnative-example` | `architecture-microservice-example` |
| **数据库** | H2 嵌入式（零外部依赖） | 外部数据库（MySQL/PG） | 外部数据库（MySQL/PG） |
| **缓存** | Caffeine 本地缓存 | Caffeine 本地缓存 | Redis 分布式缓存 |
| **存储** | 本地文件系统 | 对象存储（S3/MinIO） | 对象存储（S3/MinIO） |
| **事件** | InMemory 事件总线 | InMemory 事件总线 | 消息队列（RabbitMQ/Kafka） |
| **调度** | Quartz 进程内调度 | Quartz 进程内调度 | XXL-Job 分布式调度 |
| **服务发现** | 静态配置 | K8s Service | Nacos |
| **配置中心** | Spring Environment | K8s ConfigMap 热刷新 | Nacos Config |
| **事务** | 本地事务（Spring） | 本地事务（Spring） | 分布式事务（Seata） |
| **网关** | Servlet Filter（限流/熔断/灰度） | Service Mesh（Istio） | API Gateway |
| **监控** | 基础日志 | OpenTelemetry | OpenTelemetry |
| **外部依赖** | 零 | K8s 集群 | Nacos + MySQL + Redis + Seata |
| **启动时间** | 秒级 | 秒级 | 分钟级（依赖中间件） |
| **适用团队** | 1-5 人 | 5-20 人 | 20+ 人 |
| **适用场景** | 小微项目、Demo、快速原型 | 云原生优先、K8s 标准化 | 大规模微服务、复杂业务 |

---

## 选型决策树

```
项目启动
    │
    ├── 团队规模 < 5 人？
    │   └── 是 → 单体架构（monolith-starter）
    │       ├── 优点：零外部依赖，本地即可启动
    │       ├── 缺点：无法横向扩展
    │       └── 推荐：创业初期、内部工具、原型验证
    │
    ├── 团队已标准化 K8s？
    │   └── 是 → 云原生架构（cloudnative-starter）
    │       ├── 优点：K8s 原生适配，ConfigMap 配置热刷新，Service Mesh 流量治理
    │       ├── 缺点：强依赖 K8s 集群
    │       └── 推荐：云原生团队、SRE 体系完善
    │
    └── 需要独立部署 + 独立扩展？
        └── 是 → 微服务架构（microservice-starter）
            ├── 优点：独立部署、独立扩展、技术异构
            ├── 缺点：运维复杂度高，需要 Nacos/Seata 等中间件
            └── 推荐：大规模业务、多团队协作
```

---

## 详细对比

### 1. 单体架构（monolith-starter）

**一句话总结**: 一行依赖，零外部依赖，本地即可启动。

**典型配置**:
```yaml
aryee:
  cache:
    type: caffeine          # Caffeine 本地缓存
  transaction:
    type: local             # Spring 本地事务
  transport:
    discovery:
      type: static          # 静态配置，无注册中心
```

**Maven 依赖**（复制自 `examples/architecture-monolith-example`）**：**
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

<dependencies>
    <dependency><groupId>cn.aryee.foundation</groupId><artifactId>commons-servlet</artifactId></dependency>
    <dependency><groupId>cn.aryee.foundation</groupId><artifactId>cache-spring-boot-starter</artifactId></dependency>
    <dependency><groupId>cn.aryee.foundation</groupId><artifactId>database-spring-boot-starter</artifactId></dependency>
    <dependency><groupId>cn.aryee.foundation</groupId><artifactId>storage-spring-boot-starter</artifactId></dependency>
    <dependency><groupId>cn.aryee.foundation</groupId><artifactId>event-spring-boot-starter</artifactId></dependency>
    <dependency><groupId>cn.aryee.foundation</groupId><artifactId>scheduler-spring-boot-starter</artifactId></dependency>
    <dependency><groupId>cn.aryee.foundation</groupId><artifactId>security-spring-boot-starter</artifactId></dependency>
    <dependency><groupId>cn.aryee.foundation</groupId><artifactId>transport-spring-boot-starter</artifactId></dependency>
    <dependency><groupId>cn.aryee.foundation</groupId><artifactId>transaction-spring-boot-starter</artifactId></dependency>
</dependencies>
```

**启动命令**:
```bash
# 无需任何外部中间件，直接启动
mvn spring-boot:run
```

**适用场景**:
- 创业初期 MVP 快速验证
- 内部管理系统、后台工具
- 学习 Demo、原型开发
- 对高可用无要求的单体应用

---

### 2. 云原生架构（cloudnative-starter）

**一句话总结**: K8s 原生适配，ConfigMap 配置热刷新，一行依赖云原生开箱即用。

**典型配置**:
```yaml
aryee:
  config:
    kubernetes:
      enabled: true
      config-map-name: my-app-config
      refresh-interval-seconds: 30
  transport:
    discovery:
      type: static          # 通过 K8s Service 发现，Static 兜底
```

**Maven 依赖**（复制自 `examples/architecture-cloudnative-example`）**：**
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

<dependencies>
    <dependency><groupId>cn.aryee.foundation</groupId><artifactId>commons-spring</artifactId></dependency>
    <dependency><groupId>cn.aryee.foundation</groupId><artifactId>transport-spring-boot-starter</artifactId></dependency>
    <dependency><groupId>cn.aryee.foundation</groupId><artifactId>gateway-spring-boot-starter</artifactId></dependency>
    <dependency><groupId>cn.aryee.foundation</groupId><artifactId>monitoring-spring-boot-starter</artifactId></dependency>
    <dependency><groupId>cn.aryee.foundation</groupId><artifactId>storage-spring-boot-starter</artifactId></dependency>
    <dependency><groupId>cn.aryee.foundation</groupId><artifactId>security-spring-boot-starter</artifactId></dependency>
    <dependency><groupId>cn.aryee.foundation</groupId><artifactId>event-spring-boot-starter</artifactId></dependency>
    <dependency><groupId>cn.aryee.foundation</groupId><artifactId>scheduler-spring-boot-starter</artifactId></dependency>
</dependencies>
```

**K8s ConfigMap 示例**:
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: aryee-foundation-config
  namespace: default
data:
  application.yml: |
    aryee:
      cache:
        type: caffeine
      transaction:
        type: local
```

**适用场景**:
- 团队已标准化 K8s 部署
- 需要 ConfigMap 配置热刷新
- 使用 Service Mesh（Istio）进行流量治理
- 需要 OpenTelemetry 可观测性

---

### 3. 微服务架构（microservice-starter）

**一句话总结**: Nacos 注册中心 + Seata 分布式事务 + XXL-Job 分布式调度，一行依赖微服务治理开箱即用。

**典型配置**:
```yaml
aryee:
  cache:
    type: redis             # Redis 分布式缓存
  transaction:
    type: seata             # Seata 分布式事务
  transport:
    discovery:
      type: nacos           # Nacos 服务发现
  scheduler:
    type: xxl-job           # XXL-Job 分布式调度
```

**Maven 依赖**（复制自 `examples/architecture-microservice-example`）**：**
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

<dependencies>
    <dependency><groupId>cn.aryee.foundation</groupId><artifactId>commons-spring</artifactId></dependency>
    <dependency><groupId>cn.aryee.foundation</groupId><artifactId>cache-spring-boot-starter</artifactId></dependency>
    <dependency><groupId>cn.aryee.foundation</groupId><artifactId>database-spring-boot-starter</artifactId></dependency>
    <dependency><groupId>cn.aryee.foundation</groupId><artifactId>storage-spring-boot-starter</artifactId></dependency>
    <dependency><groupId>cn.aryee.foundation</groupId><artifactId>event-spring-boot-starter</artifactId></dependency>
    <dependency><groupId>cn.aryee.foundation</groupId><artifactId>scheduler-spring-boot-starter</artifactId></dependency>
    <dependency><groupId>cn.aryee.foundation</groupId><artifactId>security-spring-boot-starter</artifactId></dependency>
    <dependency><groupId>cn.aryee.foundation</groupId><artifactId>transport-spring-boot-starter</artifactId></dependency>
    <dependency><groupId>cn.aryee.foundation</groupId><artifactId>transaction-spring-boot-starter</artifactId></dependency>
    <dependency><groupId>cn.aryee.foundation</groupId><artifactId>monitoring-spring-boot-starter</artifactId></dependency>
    <dependency><groupId>cn.aryee.foundation</groupId><artifactId>gateway-spring-boot-starter</artifactId></dependency>
</dependencies>
```

**前置条件**:
- Nacos 2.x+ 服务（注册中心 + 配置中心）
- MySQL 5.7+ 数据库
- Redis 6.x+ 缓存
- Seata 1.8+ 事务协调器
- XXL-Job 2.4+ 调度中心（可选）

**适用场景**:
- 大规模业务系统，需要独立部署
- 各模块需要独立扩展
- 需要分布式事务保证数据一致性
- 多团队协作开发

---

## 架构迁移路径

```
单体架构 → 云原生架构 → 微服务架构
（monolith）  （cloudnative）  （microservice）
```

### 单体 → 云原生

| 变更项 | 单体 | 云原生 |
|--------|------|--------|
| Starter | `monolith-starter` | `cloudnative-starter` |
| 配置 | local YAML | K8s ConfigMap 热刷新 |
| 部署 | 单机部署 | Docker + K8s Deployment |
| 监控 | 基础日志 | OTel 链路追踪 + 指标 |

**迁移成本**: 低（仅换 Starter，配置迁移到 ConfigMap）

### 云原生 → 微服务

| 变更项 | 云原生 | 微服务 |
|--------|--------|--------|
| Starter | `cloudnative-starter` | `microservice-starter` |
| 服务发现 | K8s Service | Nacos |
| 配置中心 | K8s ConfigMap | Nacos Config |
| 事务 | 本地事务 | Seata 分布式事务 |
| 缓存 | Caffeine | Redis |
| 调度 | Quartz | XXL-Job |

**迁移成本**: 中（需要搭建 Nacos + Seata + Redis + XXL-Job）

---

## 最佳实践

### 1. 项目初期使用单体架构

复制 `examples/architecture-monolith-example` 的 pom.xml 依赖部分，引入 `bom-full` + 基础模块 Starter。

### 2. 业务代码按标准四层架构编写

```java
// Controller/Service/Repository 保持标准写法
// 切换架构形态时不需要修改业务代码
@RestController
@RequestMapping("/api/users")
public class UserController {
    // ...
}
```

### 3. 需要迁移时调整模块组合

参考目标架构的示例项目（`examples/architecture-*-example`），调整 pom.xml 中的模块 Starter 组合。例如从单体迁移到云原生：增加 `gateway-spring-boot-starter`、`monitoring-spring-boot-starter`，将 `cache` 从 Caffeine 切换为 Redis 等。

### 4. 根据场景调整配置

```yaml
aryee:
  cache:
    type: caffeine    # 单体用 Caffeine
    # type: redis    # 微服务用 Redis
  transaction:
    type: local      # 单体用本地事务
    # type: seata    # 微服务用 Seata
```

---

## 更多资源

- [跨架构迁移手册](./guides/MIGRATION_GUIDE.md) — 详细的迁移步骤与注意事项
- [单体架构示例../examples/architecture-monolith-example — monolith-starter 完整示例
- [云原生架构示例../examples/architecture-cloudnative-example — cloudnative-starter 完整示例
- [微服务架构示例../examples/architecture-microservice-example — microservice-starter 完整示例
- [架构演进计划](https://github.com/aryeecn/aryee-foundation) — 全架构演进路线图