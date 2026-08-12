# Aryee Foundation 模块能力矩阵速查表

> **文档目标**: 帮助开发者在选型阶段快速了解各模块的核心能力、适用场景和技术限制，做出正确的技术决策。
> 
> **最后更新**: 2026-08-13 | **版本**: 1.0.0

---

## 📋 目录

- [Cache 缓存模块](#cache-缓存模块)
- [Database 数据库模块](#database-数据库模块)
- [Event 事件驱动模块](#event-事件驱动模块)
- [Dict 字典模块](#dict-字典模块)
- [Tenant 多租户模块](#tenant-多租户模块)
- [Transport 服务调用模块](#transport-服务调用模块)
- [Doc API 文档模块](#doc-api-文档模块)
- [Security 安全模块](#security-安全模块)
- [WebSocket 实时通信模块](#websocket-实时通信模块)
- [Storage 文件存储模块](#storage-文件存储模块)
- [I18n 国际化模块](#i18n-国际化模块)
- [Transaction 分布式事务模块](#transaction-分布式事务模块)
- [Scheduler 任务调度模块](#scheduler-任务调度模块)
- [Monitoring 监控告警模块](#monitoring-监控告警模块)

---

## Cache 缓存模块

### 核心能力对比

| 功能特性 | Memory | Caffeine | Redis | 多级缓存 (L1+L2) |
|---------|--------|----------|-------|-----------------|
| **分布式共享** | ❌ | ❌ | ✅ | ✅ |
| **TTL 支持** | ✅ | ✅ | ✅ | ✅ |
| **随机 TTL（防雪崩）** | ❌ | ❌ | ✅ | ✅ |
| **互斥锁（防击穿）** | ❌ | ❌ | ✅ | ✅ |
| **LRU/LFU 淘汰策略** | ❌ | ✅ (Window-TinyLFU) | ❌ (LRU) | L1: LFU, L2: LRU |
| **集群广播失效** | ❌ | ❌ | ✅ (Pub/Sub) | ✅ |
| **Null 值缓存（防穿透）** | ✅ | ✅ | ✅ | ✅ |
| **内存占用** | 中 | 低（高效压缩） | 高（网络序列化） | 中（L1 小容量） |
| **适用规模** | 单机小数据量 | 单机中等数据量 | 分布式大规模 | 分布式高性能 |
| **启动依赖** | 无 | caffeine jar | spring-data-redis | 两者都需要 |

### 选型建议

```yaml
# 场景 1: 单体应用，数据量 < 10万条
aryee:
  cache:
    type: memory  # 零依赖，开箱即用

# 场景 2: 单体应用，需要高性能本地缓存
aryee:
  cache:
    type: caffeine  # Window-TinyLFU 算法，命中率更高

# 场景 3: 微服务架构，需要分布式缓存
aryee:
  cache:
    type: redis  # 需引入 spring-boot-starter-data-redis

# 场景 4: 高并发读场景（如热点商品）
aryee:
  cache:
    multi-level:
      enabled: true  # L1: Caffeine, L2: Redis
      strategy: broadcast  # Redis Pub/Sub 通知 L1 失效
```

### 生产环境注意事项

- ⚠️ **Memory/Caffeine**: 仅适用于单机场景，重启后数据丢失
- ⚠️ **Redis**: 需配置连接池、超时时间、哨兵/集群模式
- ⚠️ **多级缓存**: L1 容量建议设置为 L2 的 10%~20%，避免内存溢出

---

## Database 数据库模块

### 核心能力对比

| 功能特性 | H2 嵌入式 | MySQL/PostgreSQL | 多租户 Schema | 多租户 Database |
|---------|----------|------------------|--------------|----------------|
| **零配置启动** | ✅ | ❌ | ❌ | ❌ |
| **持久化** | 文件模式✅ / 内存模式❌ | ✅ | ✅ | ✅ |
| **生产可用** | ❌（仅开发测试） | ✅ | ✅ | ✅ |
| **租户隔离性** | N/A | 列级（弱） | Schema 级（中） | Database 级（强） |
| **迁移成本** | 低（MySQL 兼容模式） | 中 | 高 | 极高 |
| **运维复杂度** | 无 | 中 | 中高 | 高 |
| **适用场景** | 演示/原型/单元测试 | 单体应用 | SaaS 多租户 | 金融级隔离 |

### 自动检测机制

```yaml
# 未配置 spring.datasource.url 且 classpath 存在 H2 驱动时，自动启用嵌入式 H2
# 无需任何配置即可启动 CRUD 操作

# 生产环境必须显式配置外部数据源
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: secret
```

### 多租户模式选型

| 模式 | 隔离级别 | 实施难度 | 推荐场景 |
|------|---------|---------|---------|
| **column** | 行级（弱） | 低 | 内部系统、信任租户 |
| **schema** | Schema 级（中） | 中 | SaaS 平台（推荐） |
| **database** | Database 级（强） | 高 | 金融/医疗等强监管行业 |

---

## Event 事件驱动模块

### EventBus 实现对比

| 功能特性 | Memory | Kafka | RabbitMQ | RocketMQ | Redis |
|---------|--------|-------|----------|----------|-------|
| **持久化** | ❌ | ✅ | ✅ | ✅ | ❌ |
| **顺序消息** | ✅ | ✅ | ❌ | ✅ | ❌ |
| **事务消息** | ❌ | ❌ | ❌ | ✅ | ❌ |
| **延迟消息** | ❌ | ❌ | ✅ (插件) | ✅ | ✅ |
| **死信队列** | ❌ | ✅ | ✅ | ✅ | ❌ |
| **吞吐量** | 高（进程内） | 极高 | 中高 | 高 | 中 |
| **可靠性** | 低 | 高 | 高 | 极高 | 中 |
| **运维成本** | 无 | 高 | 中 | 中高 | 低 |
| **适用规模** | 单机开发测试 | 大数据/日志处理 | 微服务解耦 | 金融/订单场景 | 中小规模 |

### 选型决策树

```
需要持久化？
├─ 否 → Memory（开发测试）或 Redis（轻量级）
└─ 是 ↓
    需要事务消息？
    ├─ 是 → RocketMQ
    └─ 否 ↓
        需要严格顺序？
        ├─ 是 → Kafka 或 RocketMQ
        └─ 否 ↓
            需要灵活路由（Exchange/Queue）？
            ├─ 是 → RabbitMQ
            └─ 否 → Kafka（高吞吐）或 RocketMQ（高可靠）
```

### 配置示例

```yaml
# 开发环境：内存队列
aryee:
  event:
    type: memory

# 微服务架构：RabbitMQ
aryee:
  event:
    type: rabbitmq
    reliability-enabled: true
    reliability-max-retries: 3

# 金融场景：RocketMQ 事务消息
aryee:
  event:
    type: rocketmq
    reliability-enabled: true
    security-enabled: true
    sign-events: true
```

---

## Dict 字典模块

### 实现类型对比

| 功能特性 | Memory | Cached | Database |
|---------|--------|--------|----------|
| **持久化** | ❌ | ❌ | ✅ |
| **分布式共享** | ❌ | ✅ | ✅ |
| **动态管理（REST API）** | ❌ | ❌ | ✅ |
| **变更事件广播** | ❌ | ✅ | ✅ |
| **枚举自动绑定** | ✅ | ✅ | ✅ |
| **多租户支持** | ❌ | ✅ | ✅ |
| **启动依赖** | 无 | cache 模块 | database 模块 |
| **适用场景** | 静态字典/开发测试 | 只读字典/微服务 | 动态管理/SaaS |

### 枚举自动绑定示例

```java
// 定义枚举
@DictEnumBinding(dictType = "user_status")
public enum UserStatus {
    ACTIVE(1, "激活"),
    DISABLED(0, "禁用");
}

// 自动从数据库加载字典项并绑定到枚举
// 前端返回时自动翻译为中文描述
```

---

## Tenant 多租户模块

### 核心能力

| 功能 | 说明 | 状态 |
|------|------|------|
| **租户上下文传递** | ThreadLocal / Reactive Context | ✅ |
| **数据隔离拦截器** | MyBatis-Plus / JSQLParser | ✅ |
| **租户级缓存隔离** | `aryee.tenant.cache.enabled` | ✅ |
| **租户自助管理** | REST API（创建/删除/查询） | 🔲 待实现 |
| **数据迁移工具** | 单租户 → 多租户迁移脚本 | 🔲 待实现 |
| **审计日志** | 租户操作记录 | 🔲 待实现 |

### 数据隔离策略

```yaml
aryee:
  tenant:
    multi-tenant:
      enabled: true
      mode: schema  # schema / database / column
      tenant-id-column: tenant_id
      default-tenant-id: default
```

---

## Transport 服务调用模块

### Starter 选型指南

| Starter | 包含依赖 | 适用架构 | 典型场景 |
|---------|---------|---------|---------|
| **transport-spring-boot-starter** | 基础 HTTP 客户端 | 单体/微服务 | 简单 REST 调用 |
| **transport-microservice-spring-boot-starter** | + Feign + LoadBalancer + Resilience4j | 微服务 | 服务间调用 + 熔断重试 |
| **transport-reactive-spring-boot-starter** | WebClient | 响应式单体 | 非阻塞 HTTP 调用 |
| **transport-reactive-microservice-spring-boot-starter** | + Spring Cloud LoadBalancer | 响应式微服务 | 响应式服务网格 |

### 出站熔断配置

```yaml
aryee:
  transport:
    outbound:
      circuit-breaker:
        enabled: true
        failure-rate-threshold: 50  # 失败率阈值 %
        wait-duration-in-open-state: 10s  # 熔断打开等待时间
        sliding-window-size: 100  # 滑动窗口大小
```

---

## Doc API 文档模块

### Starter 选型指南

| Starter | 包含依赖 | 适用架构 | 典型场景 |
|---------|---------|---------|---------|
| **doc-spring-boot-starter** | SpringDoc OpenAPI | 单体/微服务 | 基础 API 文档 |
| **doc-microservice-spring-boot-starter** | + Spring Cloud Commons + Gateway Server | 微服务网关 | 聚合下游服务文档 |
| **doc-reactive-spring-boot-starter** | SpringDoc WebFlux | 响应式单体 | 响应式 API 文档 |
| **doc-reactive-microservice-spring-boot-starter** | + Gateway Server | 响应式微服务 | 响应式网关文档聚合 |

### 访问地址

```
Swagger UI:  http://localhost:8080/swagger-ui.html
API Docs:    http://localhost:8080/v3/api-docs
Knife4j UI:  http://localhost:8080/doc.html
```

---

## Security 安全模块

### 认证适配器对比

| 适配器 | 特点 | 适用场景 |
|--------|------|---------|
| **Local (JWT)** | 无状态 Token，框架内置 | 单体/简单微服务 |
| **Sa-Token** | 国产轻量级会话管理 | 国内项目、需要会话管理 |
| **Keycloak** | 企业级 IAM，OIDC/OAuth2 | 大型企业、SSO 需求 |
| **OAuth2** | 标准 OAuth2 协议 | 第三方登录、开放平台 |

### Feign 安全上下文传递

```yaml
# 微服务间自动传递 userId/tenantId
aryee:
  security:
    outbound-enabled: true  # 启用 Feign 拦截器
```

---

## WebSocket 实时通信模块

### 核心能力

| 功能 | 说明 | 状态 |
|------|------|------|
| **集群消息广播** | 通过 Messaging Pub/Sub 中继 | ✅ |
| **离线消息持久化** | 存储未送达消息 | 🔲 待实现 |
| **多租户广播隔离** | `broadcastToTenant()` | 🔲 待实现 |
| **注解驱动监听器** | `@OnMessage` | 🔲 待实现 |
| **消息 ACK 与重传** | 可靠投递保障 | 🔲 待实现 |
| **连接/消息指标** | 接入 Monitoring | 🔲 待实现 |

### 集群广播配置

```yaml
aryee:
  websocket:
    cluster:
      enabled: true  # 启用集群广播（需引入 messaging 模块）
```

---

## Storage 文件存储模块

### 存储后端对比

| 后端 | 特点 | 适用场景 |
|------|------|---------|
| **Local** | 本地文件系统 | 开发测试/小规模 |
| **MinIO** | S3 兼容对象存储 | 私有云/自建存储 |
| **Aliyun OSS** | 阿里云对象存储 | 阿里云生态 |
| **AWS S3** | 亚马逊对象存储 | 海外业务 |

### 缩略图生成

```yaml
aryee:
  storage:
    thumbnail:
      enabled: true
      widths: [200, 400, 800]  # 生成多种尺寸
      quality: 0.8  # JPEG 质量
```

---

## I18n 国际化模块

### 翻译源对比

| 翻译源 | 特点 | 适用场景 |
|--------|------|---------|
| **Properties 文件** | 静态资源，编译时打包 | 固定文案 |
| **数据库动态翻译** | 运行时可修改，支持热更新 | 🔲 待实现 |
| **模板翻译** | 变量替换 + 多语言模板 | 🔲 待实现 |
| **REST 管理 API** | 在线管理翻译条目 | 🔲 待实现 |

---

## Transaction 分布式事务模块

### 事务模式对比

| 模式 | 一致性 | 性能 | 适用场景 |
|------|--------|------|---------|
| **AT 模式** | 最终一致 | 高 | 大多数场景（推荐） |
| **TCC 模式** | 强一致 | 中 | 需要精确控制 |
| **Saga 模式** | 最终一致 | 高 | 长事务流程 |
| **XA 模式** | 强一致 | 低 | 传统数据库场景 |

### Seata 集成

```yaml
spring:
  cloud:
    alibaba:
      seata:
        tx-service-group: my_tx_group
```

---

## Scheduler 任务调度模块

### 调度器对比

| 调度器 | 特点 | 适用场景 |
|--------|------|---------|
| **XXL-JOB** | 分布式调度中心，Web 管理界面 | 企业级定时任务 |
| **Quartz** | 成熟稳定，支持集群 | 传统单体应用 |
| **Spring Task** | 轻量级，@Scheduled 注解 | 简单定时任务 |

---

## Monitoring 监控告警模块

### 监控维度

| 维度 | 指标 | 集成方式 |
|------|------|---------|
| **JVM** | GC/内存/CPU | Micrometer + Prometheus |
| **HTTP** | QPS/RT/错误率 | Actuator + Micrometer |
| **缓存** | 命中率/ eviction | CacheMetricsService |
| **数据库** | 连接池/慢查询 | DataSource Metrics |
| **链路追踪** | TraceId/Span | Micrometer Tracing + Zipkin |

### 告警渠道

| 渠道 | 状态 |
|------|------|
| **钉钉机器人** | ✅ |
| **企业微信** | 🔲 待实现 |
| **邮件** | 🔲 待实现 |
| **Webhook** | ✅ |

---

## 🎯 快速选型决策树

### 场景 1: 单体应用（小型项目）

```
技术栈选择:
├─ Cache: Memory（零配置）
├─ Database: H2 嵌入式（开发）→ MySQL（生产）
├─ Event: Memory（开发）→ 不启用（单体无需事件总线）
├─ Dict: Memory（静态字典）
├─ Security: Local JWT
└─ Doc: doc-spring-boot-starter
```

### 场景 2: 微服务架构（中型项目）

```
技术栈选择:
├─ Cache: Redis（分布式共享）
├─ Database: MySQL + 多租户 Schema 模式
├─ Event: RabbitMQ（服务解耦）
├─ Dict: Database（动态管理）
├─ Tenant: Schema 隔离 + 缓存隔离
├─ Transport: transport-microservice-spring-boot-starter
├─ Security: Sa-Token 或 Keycloak
├─ Doc: doc-microservice-spring-boot-starter（网关聚合）
└─ Monitoring: Prometheus + Grafana
```

### 场景 3: 高并发互联网应用

```
技术栈选择:
├─ Cache: 多级缓存（Caffeine L1 + Redis L2）
├─ Database: MySQL 分库分表 + 读写分离
├─ Event: Kafka（高吞吐）
├─ WebSocket: 集群广播 + 离线消息持久化
├─ Storage: MinIO/Aliyun OSS + CDN
├─ Transport: 出站熔断 + 限流
└─ Monitoring: 全链路追踪 + 实时告警
```

---

## 📝 更新记录

| 版本 | 日期 | 更新内容 |
|------|------|---------|
| 1.0.0 | 2026-08-13 | 初始版本，覆盖 14 个核心模块 |

---

## 💡 反馈与建议

如发现文档错误或需要补充模块能力信息，请提交 Issue 至：
- GitHub: https://github.com/aryee/aryee-foundation-docs/issues
