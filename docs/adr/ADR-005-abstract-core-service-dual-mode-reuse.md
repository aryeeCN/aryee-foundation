# ADR-005: AbstractCoreService 双模式代码复用

**日期**: 2026-08-04  
**状态**: 已接受  
**决策者**: Aryee  

## 背景

Aryee Foundation 采用 Blocking / Reactive 双模式隔离架构（见 [ADR-002](ADR-002-blocking-reactive-isolation.md)）。
在 storage、cache、transport 等模块的实现中，Blocking 和 Reactive 版本存在大量**非 IO 业务逻辑重复**：

- 文件 ID 生成（`UUID.randomUUID().toString()`）
- 文件扩展名提取
- 存储文件名构造（`fileId + "." + extension`）
- 对象 Key 构造（`directory + "/" + storedFileName`）
- 目标 Bucket 解析（`bucket != null ? bucket : defaultBucket`）
- Content-Type 推断
- FileInfo 构建
- 服务生命周期管理（初始化、销毁、健康检查）

以 storage 模块的 `LocalFileService`（Blocking）和 `ReactiveLocalFileService`（Reactive）为例，
两个类中 `getFileExtension`、`buildFileUrl`、`buildFileInfo`、`initStoragePath` 等方法几乎完全相同，
仅 IO 调用方式不同（直接调用 vs `Mono.fromCallable` 包装）。

重复代码带来的问题：
1. **维护成本高**：修改公共逻辑需同步修改两处，容易遗漏
2. **一致性风险**：Blocking 和 Reactive 版本可能逐渐产生行为差异
3. **测试覆盖不均**：同一逻辑需在两个模式中分别测试

## 决策

引入两层抽象基类提取公共业务逻辑：

### 1. `AbstractCoreService<T>`（commons-core）

通用服务模板基类，实现 `CoreService<T>` 接口，提供：
- **线程安全的生命周期管理**：`AtomicBoolean` 保证 `initialize()` / `destroy()` 幂等
- **模板方法模式**：`doInitialize()` / `doDestroy()` / `doProcess()` / `validateItem()` 钩子
- **状态跟踪**：初始化状态、处理计数器
- **元数据管理**：服务名称、版本、描述
- **统计信息**：可扩展的 `getExtraStatistics()` 钩子

### 2. `AbstractStorageService`（storage-infrastructure）

存储模块专用抽象基类，提取 Blocking / Reactive 存储实现之间的公共非 IO 逻辑：
- `generateFileId()` — UUID 生成
- `getFileExtension(String)` — 扩展名提取（空安全）
- `buildStoredFileName(String, String)` — 存储文件名构造
- `buildObjectKey(String, String)` — 对象 Key 构造（自动处理尾部斜杠）
- `resolveTargetBucket(String, String)` — Bucket 解析（空安全回退）
- `extractFileIdFromFileName(String)` — 从文件名提取 fileId
- `extractFileIdFromObjectKey(String)` — 从对象 Key 提取 fileId（去路径前缀 + 扩展名）
- `getContentType(String)` — Content-Type 推断（覆盖 30+ 常见类型）
- `buildBasicFileInfo(...)` — 基础 FileInfo 构建（不含 IO 字段）
- `buildFileInfo(...)` — 完整 FileInfo 构建（含 fileSize 和 contentType）

### 设计原则

- **纯业务逻辑**：抽象基类不包含任何 IO 操作，仅处理数据转换和规则计算
- **IO 隔离**：Blocking 子类直接调用 IO API，Reactive 子类用 `Mono.fromCallable` 包装
- **不强制继承**：实现类可选择继承抽象基类或直接实现接口，保持灵活性
- **不过度抽象**：仅在重复度高的方法上提取，配置相关的特有方法（如 `buildFileUrl`）保留在子类

## 理由

1. **DRY 原则**：消除 4 个存储实现 × 2 个模式 = 8 个类中的重复代码
2. **一致性保证**：公共逻辑修改只需一处，Blocking / Reactive 行为自动同步
3. **测试效率**：抽象基类的逻辑只需一次测试，子类测试聚焦 IO 适配
4. **扩展性**：新增存储类型（如 S3、Azure Blob）可直接继承复用公共逻辑
5. **符合架构规则**：不违反 Blocking / Reactive 隔离原则，抽象基类不含 IO 类型

## 影响

### 已重构的类

| 模块 | 类 | 变更 |
|------|-----|------|
| commons-core | `AbstractCoreService` | 新增 |
| storage-infrastructure | `AbstractStorageService` | 新增 |
| storage-infrastructure | `LocalFileService` | 继承 `AbstractStorageService`，移除 3 个重复方法 |
| storage-infrastructure | `ReactiveLocalFileService` | 继承 `AbstractStorageService`，移除 3 个重复方法 |
| storage-infrastructure | `OssFileService` | 继承 `AbstractStorageService`，移除 3 个重复方法 |
| storage-infrastructure | `ReactiveOssFileService` | 继承 `AbstractStorageService`，移除 3 个重复方法 |

### 后续可扩展

- cache 模块可引入 `AbstractCacheService` 提取 key prefix、TTL 随机化等公共逻辑
- transport 模块可引入 `AbstractTransportService` 提取端点解析、超时配置等公共逻辑
- 其他云存储实现（MinIO/COS/Qiniu）可逐步迁移到继承 `AbstractStorageService`

### 测试覆盖

- `AbstractCoreServiceTest`：15 个测试用例（生命周期、模板方法、元数据、统计）
- `AbstractStorageServiceTest`：28 个测试用例（ID 生成、扩展名、Key 构造、Bucket 解析、Content-Type、FileInfo 构建）
