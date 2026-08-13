# 文档中心

Aryee Foundation 官方文档索引。框架以二进制构件发布到 Maven Central（groupId `cn.aryee.foundation`），本仓库包含全部使用文档、架构指南与示例代码。

## 入门

| 文档 | 说明 |
|------|------|
| [快速开始](getting-started.md) | 环境要求、快照仓配置、引入依赖、第一个功能、运行示例 |
| [示例代码](../examples/README.md) | 30+ 可运行示例工程（含三大架构形态完整示例） |

## 架构指南

| 文档 | 说明 |
|------|------|
| [架构选型指南](guides/ARCHITECTURE_SELECTION_GUIDE.md) | 单体 / 微服务 / 云原生三大形态的选型决策 |
| [跨架构迁移手册](guides/MIGRATION_GUIDE.md) | 已有项目接入框架与架构形态切换 |

## 模块文档

### 公共基础

| 模块 | 说明 |
|------|------|
| [commons](modules/commons.md) | 公共基础模块（commons-core/domain/spring/web）：异常/响应/分页/ID/加密/工具 |
| [bom](modules/bom.md) | 依赖版本管理（bom-base/bom-internal/bom-full + 按模块 BOM） |

### 功能模块

| 模块 | 说明 |
|------|------|
| [cache](modules/cache.md) | 缓存（Redis + Caffeine + 多级缓存 + 分布式锁） |
| [database](modules/database.md) | 数据库（MyBatis-Plus + JPA + R2DBC + H2 嵌入式默认） |
| [security](modules/security.md) | 安全（本地 JWT 核心 + Sa-Token/Keycloak/OAuth 可插拔适配器 + 幂等 + MFA） |
| [storage](modules/storage.md) | 存储（Local/OSS/COS/Qiniu/MinIO） |
| [messaging](modules/messaging.md) | 消息（Kafka/RabbitMQ/RocketMQ，收发完整） |
| [transport](modules/transport.md) | 传输（入站 Web + 出站 OpenFeign/WebClient，多注册中心 SPI） |
| [monitoring](modules/monitoring.md) | 监控（指标/链路/日志/健康/告警渠道） |
| [scheduler](modules/scheduler.md) | 调度（Quartz + XXL-Job） |
| [event](modules/event.md) | 事件（InMemory + Kafka/RabbitMQ/RocketMQ 背板） |
| [transaction](modules/transaction.md) | 事务（Local 默认 + Seata 可选，统一 @AryeeTransactional 门面） |
| [sync](modules/sync.md) | 同步（分布式锁 + 数据同步） |
| [tenant](modules/tenant.md) | 多租户（JDBC + R2DBC 隔离） |
| [ai](modules/ai.md) | AI（LLM + RAG + Agent + Embedding + VectorStore） |
| [gateway](modules/gateway.md) | 网关增强（限流/熔断/灰度/聚合，MVC + Reactive 双栈） |
| [apidoc](modules/doc.md) | API 文档（OpenAPI 分组 + 安全方案声明） |
| [dict](modules/dict.md) | 字典管理 |
| [websocket](modules/websocket.md) | WebSocket 实时通信 |
| [i18n](modules/i18n.md) | 国际化（5 种 Locale 解析策略） |
| [workflow](modules/workflow.md) | 工作流（Flowable 集成） |

### 工具模块

| 模块 | 说明 |
|------|------|
| [cli](modules/cli.md) | 脚手架工具（基于 picocli）：项目校验、依赖分析、代码生成、加解密命令 |

## 架构决策记录（ADR）

| 文档 | 主题 |
|------|------|
| [ADR-001](adr/ADR-001-modular-three-layer-architecture.md) | 模块化三层架构 |
| [ADR-002](adr/ADR-002-blocking-reactive-isolation.md) | Blocking / Reactive 隔离 |
| [ADR-003a](adr/ADR-003-bom-dependency-management.md) | BOM 依赖管理 |
| [ADR-003b](adr/ADR-003-bom-layered-version-management.md) | BOM 分层版本管理 |
| [ADR-004a](adr/ADR-004-unified-exception-system.md) | 统一异常系统 |
| [ADR-004b](adr/ADR-004-global-exception-hierarchy.md) | 全局异常层次 |
| [ADR-005a](adr/ADR-005-abstract-core-service-dual-mode-reuse.md) | 抽象核心服务双模式复用 |
| [ADR-005b](adr/ADR-005-rbac-abac-hybrid-permission.md) | RBAC + ABAC 混合权限 |

## 反馈

文档勘误、使用问题请到 [GitHub Issues](https://github.com/aryeecn/aryee-foundation/issues) 反馈，或直接提交 Pull Request。
