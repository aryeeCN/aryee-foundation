# ADR-001: 模块化三层架构设计

## 状态
✅ 已采纳

## 背景
框架需要一个清晰、可扩展、可维护的架构结构。目标是支持多个功能模块（cache、database、security、ai 等），每个模块独立演进，同时保持统一的设计规范。

## 决策
采用 **模块化 + 三层架构** 设计，每个功能模块遵循统一的三层模式：

- **API 层** (`{module}-api`)：对外暴露的契约层，定义接口、注解、枚举、DTO/Model，禁止包含业务逻辑实现
- **Infrastructure 层** (`{module}-infrastructure`)：技术实现层，提供 Blocking/Reactive 双模式实现
- **Starter 层**（`{module}-spring-boot-autoconfigure` + `{module}-spring-boot-starter`）：Spring Boot 集成层

## 影响
- 每个模块独立版本、独立发布
- 依赖方向严格：Starter → Autoconfigure → Infrastructure → API
- API 层零实现、零 Spring 容器依赖（除 @ConfigurationProperties 外）
- 新增模块必须按此结构创建，确保一致性
