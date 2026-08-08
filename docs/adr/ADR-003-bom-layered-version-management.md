# ADR-003: BOM 分层依赖版本管理

## 状态
已接受（Accepted）

## 日期
2026-01-01

## 背景
框架包含 20+ 功能模块，每个模块有多个子模块，还依赖大量第三方库。需要决定依赖版本管理策略，避免版本冲突和硬编码。

## 决策
采用**4 层 BOM**分层管理：

| BOM | 职责 |
|-----|------|
| `bom-base` | 所有第三方依赖版本唯一来源 |
| `bom-{module}` | 按功能聚合，import bom-base，管理本模块内部子模块版本 |
| `bom-internal` | 管理所有内部模块版本 |
| `bom-full` | 合并 bom-base + bom-internal，对外发布 |

**规则**：
- 所有版本号定义在 `bom-base` 的 `<properties>` 中
- 子模块 POM 中禁止出现 `<version>` 标签
- 插件版本统一在 `parent` POM 的 `<pluginManagement>` 中声明

## 理由
1. **唯一来源**：版本号只在一处定义，升级只需改一个 property
2. **按需引入**：业务项目可以只 import `bom-cache`，避免全量依赖污染
3. **分层复用**：`bom-{module}` import `bom-base`，不重复声明第三方版本

## 影响
- 新增第三方依赖必须先在 `bom-base` 声明 property + dependencyManagement
- 新增功能模块必须创建 `bom-{module}` 并注册到 `bom-internal`
- 禁止在子模块 POM 中硬编码版本号
