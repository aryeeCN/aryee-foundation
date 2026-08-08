# Aryee Foundation Dict

> **所属项目**: [Aryee Foundation](../../README.md)
> **类型**: 字典管理基础设施模块
> **技术栈**: Java 21, Spring Boot 4.0.7, Jackson, MyBatis-Plus（可选）, Caffeine（可选）, Redis（可选）
> **主包**: `cn.aryee.dict.api`

字典管理基础设施模块，提供统一的字典 CRUD、缓存与注解驱动的序列化翻译能力，支持 Blocking 与 Reactive 双模式。

## 核心特性

- ✅ **三实现**: InMemory（默认）+ Cached（分布式缓存）+ Database（数据库持久化 + 缓存加速）
- ✅ **双模式**: Blocking（`DictService`）+ Reactive（`ReactiveDictService`）接口完全对齐
- ✅ **多租户隔离**: 缓存键租户感知 + 数据库租户隔离（复用 tenant 模块）
- ✅ **变更广播**: 字典写操作后通过 event 模块发布变更事件，多节点缓存自动同步
- ✅ **枚举自动绑定**: `@DictEnumBinding` 注解 + classpath 扫描，自动将 Java 枚举注册为字典源
- ✅ **i18n 联动**: `getLabel()` 优先查 i18n 消息，未命中时回退到数据库中的 itemLabel
- ✅ **注解翻译**: `@DictTranslation` 注解，Jackson 序列化时自动将字典值翻译为标签
- ✅ **导入导出**: CSV（Excel 可直接打开）批量导入/导出，零第三方依赖
- ✅ **变更审计**: 字典写操作自动委托 security 模块 `SecurityAuditService` 记录审计日志
- ✅ **REST 管理 API**: 开箱即用的字典管理端点（查询/保存/删除/导入/导出）
- ✅ **条件装配**: 根据 `aryee.dict.type` 自动选择实现，所有新增依赖均为 optional

## 快速开始

### 1. 引入依赖

#### Blocking 模式（WebMVC）

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>dict-spring-boot-starter</artifactId>
</dependency>
```

#### Reactive 模式（WebFlux）

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>dict-reactive-spring-boot-starter</artifactId>
</dependency>
```

#### 数据库持久化模式（追加 database + cache 依赖）

```xml
<!-- Blocking + 数据库持久化字典 -->
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>dict-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>database-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>cache-spring-boot-starter</artifactId>
</dependency>
```

#### 多租户模式（追加 tenant 依赖）

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>tenant-spring-boot-starter</artifactId>
</dependency>
```

### 2. 配置

```yaml
aryee:
  dict:
    enabled: true                    # 是否启用字典模块（默认 true）
    type: memory                     # 实现类型：memory | cached | database（默认 memory）
    cache-enabled: true              # 是否启用缓存（type=cached/database 时生效）
    cache-ttl: 3600                  # 缓存 TTL（秒，默认 3600）
    translation-enabled: true        # 是否启用 @DictTranslation 注解翻译（默认 true）
    translation-default-label-suffix: Label  # 翻译后标签字段后缀（默认 "Label"）
    translation-fail-fast: false     # 翻译失败时是否抛出异常（默认 false，降级返回原值）
    tenant-isolation: false          # 是否启用租户隔离（默认 false）
    event:
      enabled: false                 # 是否启用变更广播（默认 false，需引入 event 模块）
      event-type: dict.change        # 事件类型标识
    enum-scan:
      enabled: false                 # 是否启用枚举自动扫描（默认 false）
      base-packages:                 # 扫描的包路径
        - cn.aryee.business.enums
    i18n:
      enabled: false                 # 是否启用 i18n 联动（默认 false，需引入 i18n 模块）
      key-pattern: "{0}.{1}"         # i18n key 格式（{0}=dictCode, {1}=itemValue）
```

### 3. 使用字典服务

#### Blocking 模式

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final DictService dictService;

    public void initGenderDict() {
        // 新增字典项
        dictService.saveItem(DictItem.of("gender", "1", "男"));
        dictService.saveItem(DictItem.of("gender", "0", "女"));
    }

    public String getGenderLabel(String value) {
        // 查询标签
        return dictService.getLabel("gender", value);
    }

    public List<DictItem> listGenders() {
        // 获取所有启用的字典项
        return dictService.listByCode("gender");
    }
}
```

#### Reactive 模式

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final ReactiveDictService dictService;

    public Mono<Void> initGenderDict() {
        return dictService.saveItem(DictItem.of("gender", "1", "男"))
                .then(dictService.saveItem(DictItem.of("gender", "0", "女")))
                .then();
    }

    public Mono<String> getGenderLabel(String value) {
        return dictService.getLabel("gender", value);
    }

    public Flux<DictItem> listGenders() {
        return dictService.listByCode("gender");
    }
}
```

### 4. 使用 @DictTranslation 注解（Blocking 模式）

```java
@Data
public class UserVO {

    // gender 字段值为 1/0，序列化时自动查询 gender 字典，
    // 将标签（如"男"/"女"）写入 genderLabel 字段
    @DictTranslation(dictCode = "gender", labelField = "genderLabel")
    private String gender;

    // 序列化时自动生成的标签字段
    private String genderLabel;

    // 不指定 labelField 时，默认使用"原字段名 + Label"后缀
    @DictTranslation(dictCode = "user_status")
    private Integer status;

    private String statusLabel;
}
```

序列化结果：

```json
{
  "gender": "1",
  "genderLabel": "男",
  "status": 1,
  "statusLabel": "启用"
}
```

> **注意**: `@DictTranslation` 注解翻译仅在 Blocking 模式下自动启用。
> Reactive 模式下不自动注册翻译 Module（避免阻塞 Reactor 线程），
> 如需在 Reactive 环境使用，请手动注册 `DictTranslationModule` 并提供非阻塞的 `DictService` 实现。

## 配置项说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `aryee.dict.enabled` | 是否启用字典模块 | `true` |
| `aryee.dict.type` | 实现类型：`memory` / `cached` / `database` | `memory` |
| `aryee.dict.cache-enabled` | 是否启用缓存（type=cached/database 时生效） | `true` |
| `aryee.dict.cache-ttl` | 缓存 TTL（秒） | `3600` |
| `aryee.dict.translation-enabled` | 是否启用 @DictTranslation 注解翻译 | `true` |
| `aryee.dict.translation-default-label-suffix` | 翻译后标签字段后缀 | `Label` |
| `aryee.dict.translation-fail-fast` | 翻译失败时是否抛出异常 | `false` |
| `aryee.dict.tenant-isolation` | 是否启用租户隔离（缓存键包含 tenantId） | `false` |
| `aryee.dict.event.enabled` | 是否启用变更广播（需引入 event 模块） | `false` |
| `aryee.dict.event.event-type` | 事件类型标识 | `dict.change` |
| `aryee.dict.enum-scan.enabled` | 是否启用枚举自动扫描 | `false` |
| `aryee.dict.enum-scan.base-packages` | 扫描的包路径列表 | `[]` |
| `aryee.dict.i18n.enabled` | 是否启用 i18n 联动（需引入 i18n 模块） | `false` |
| `aryee.dict.i18n.key-pattern` | i18n key 格式（{0}=dictCode, {1}=itemValue） | `{0}.{1}` |
| `aryee.dict.audit.enabled` | 是否启用变更审计（需引入 security 模块） | `true` |
| `aryee.dict.rest.enabled` | 是否启用字典管理 REST API（需 WebMVC 环境） | `true` |
| `aryee.dict.rest.base-path` | REST 基础路径 | `/aryee/dict` |

## 实现类型对比

| 实现类型 | 类名 | 适用场景 | 依赖 | 分布式 |
|----------|------|----------|------|--------|
| InMemory | `InMemoryDictServiceImpl` / `ReactiveInMemoryDictServiceImpl` | 单机、测试 | 无 | ❌ |
| Cached | `CachedDictServiceImpl` / `ReactiveCachedDictServiceImpl` | 纯缓存生产环境 | cache-spring-boot-starter | ✅ |
| Database | `DatabaseDictServiceImpl` / `ReactiveDatabaseDictServiceImpl` | 数据库持久化 + 缓存加速 | database + cache starter | ✅ |

### 数据结构（Cached 实现）

字典项以 Redis Hash 结构存储：

```
key:   aryee:dict:{dictCode}        # 如 aryee:dict:gender
field: itemValue                     # 如 "1"
value: DictItem 对象（JSON 序列化）   # 如 {"dictCode":"gender","itemValue":"1","itemLabel":"男","sort":0,"enabled":true}
```

## @DictTranslation 注解参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `dictCode` | 字典编码（必填） | - |
| `labelField` | 翻译后标签写入的字段名 | `""`（使用"原字段名 + Label 后缀"） |
| `defaultValue` | 字典项不存在时的默认标签 | `""`（不写入标签字段） |
| `translateNull` | 是否翻译 null 值 | `false` |

## 导入导出与 REST 管理

### CSV 导入导出

通过 `DictImportExportService` 实现批量导入导出（RFC 4180 格式，Excel 可直接打开），
列顺序：`itemValue,itemLabel,sort,enabled,remark`。导入时首行表头自动跳过，
单项解析失败跳过不影响其余项。

```java
// 导出
String csv = importExportService.exportDict("gender");
// 导入
int count = importExportService.importDict("gender", csvContent);
```

### REST 管理端点（默认开启，生产环境请自行添加管理员鉴权）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `{base-path}/{dictCode}/items` | 查询启用项 |
| GET | `{base-path}/{dictCode}/items/all` | 查询全部项（含禁用） |
| POST | `{base-path}/{dictCode}/items` | 新增/更新字典项（upsert） |
| DELETE | `{base-path}/{dictCode}/items/{itemValue}` | 删除字典项 |
| DELETE | `{base-path}/{dictCode}` | 删除整个字典 |
| GET | `{base-path}/{dictCode}/export` | 导出 CSV（附件下载） |
| POST | `{base-path}/{dictCode}/import` | 从 CSV 文本批量导入 |

## 错误码

| 错误码 | 枚举 | 说明 |
|--------|------|------|
| 26001 | `DICT_NOT_FOUND` | 字典不存在 |
| 26002 | `DICT_ITEM_NOT_FOUND` | 字典项不存在 |
| 26003 | `DICT_CODE_DUPLICATE` | 字典编码已存在 |
| 26004 | `DICT_ITEM_VALUE_DUPLICATE` | 字典项值已存在 |
| 26005 | `DICT_DISABLED` | 字典已禁用 |
| 26006 | `DICT_ITEM_DISABLED` | 字典项已禁用 |
| 26007 | `DICT_TRANSLATION_FAILED` | 字典翻译失败 |
| 26008 | `DICT_CACHE_OPERATION_FAILED` | 字典缓存操作失败 |
| 26009 | `DICT_PERSISTENCE_FAILED` | 字典持久化失败 |
| 26010 | `DICT_EVENT_PUBLISH_FAILED` | 字典变更事件发布失败 |
| 26011 | `DICT_ENUM_SCAN_FAILED` | 字典枚举扫描失败 |
| 26012 | `DICT_IMPORT_FAILED` | 字典导入失败 |

## 模块结构

```
aryee-foundation-dict/
├── pom.xml                                       ← 聚合 POM
├── dict-api/                                     ← API 层
│   └── src/main/java/cn/aryee/dict/api/
│       ├── annotation/
│       │   ├── DictTranslation.java              ← @DictTranslation 注解
│       │   └── DictEnumBinding.java              ← @DictEnumBinding 枚举绑定注解
│       ├── config/
│       │   └── DictProperties.java               ← 配置属性（aryee.dict）
│       ├── constant/
│       │   └── DictConstants.java                ← 常量
│       ├── enums/
│       │   └── DictErrorCodeEnum.java            ← 错误码枚举（26000~26999）
│       ├── event/
│       │   └── DictChangeEvent.java              ← 字典变更事件模型
│       ├── exception/
│       │   └── DictException.java                ← 字典业务异常
│       ├── model/
│       │   └── DictItem.java                     ← 字典项模型
│       ├── repository/
│       │   ├── DictTypeRepository.java           ← Blocking 字典类型仓储
│       │   ├── DictItemRepository.java           ← Blocking 字典项仓储
│       │   ├── ReactiveDictTypeRepository.java   ← Reactive 字典类型仓储
│       │   └── ReactiveDictItemRepository.java   ← Reactive 字典项仓储
│       └── service/
│           ├── DictService.java                  ← Blocking 接口
│           ├── ReactiveDictService.java          ← Reactive 接口
│           └── DictEnumRegistry.java             ← 枚举注册表接口
├── dict-infrastructure/                          ← Infrastructure 层
│   └── src/main/java/cn/aryee/dict/infrastructure/
│       ├── blocking/
│       │   ├── memory/
│       │   │   └── InMemoryDictServiceImpl.java  ← Blocking InMemory 实现
│       │   ├── cached/
│       │   │   └── CachedDictServiceImpl.java    ← Blocking Cached 实现
│       │   └── database/
│       │       ├── entity/
│       │       │   ├── DictTypeEntity.java       ← 字典类型数据库实体
│       │       │   └── DictItemEntity.java       ← 字典项数据库实体
│       │       ├── mapper/
│       │       │   ├── DictTypeMapper.java       ← MyBatis Mapper
│       │       │   └── DictItemMapper.java       ← MyBatis Mapper
│       │       ├── repository/
│       │       │   ├── DictTypeRepositoryImpl.java ← 字典类型仓储实现
│       │       │   └── DictItemRepositoryImpl.java ← 字典项仓储实现
│       │       └── DatabaseDictServiceImpl.java  ← Blocking Database 实现
│       ├── reactive/
│       │   ├── memory/
│       │   │   └── ReactiveInMemoryDictServiceImpl.java
│       │   ├── cached/
│       │   │   └── ReactiveCachedDictServiceImpl.java
│       │   └── database/
│       │       └── ReactiveDatabaseDictServiceImpl.java ← Reactive Database 实现
│       ├── listener/
│       │   └── DictChangeListener.java           ← 字典变更事件监听器
│       ├── scanner/
│       │   ├── DictEnumScanner.java              ← 枚举扫描器
│       │   └── DictEnumRegistryImpl.java         ← 枚举注册表实现
│       ├── serializer/
│       │   ├── DictLabelPropertyWriter.java      ← Jackson 虚拟属性 Writer
│       │   ├── DictTranslationSerializerModifier.java  ← BeanSerializerModifier
│       │   └── DictTranslationModule.java        ← Jackson Module
│       └── resources/sql/
│           ├── dict_schema.sql                   ← 建表 SQL（无租户）
│           └── dict_schema_tenant.sql            ← 建表 SQL（含租户隔离）
├── dict-spring-boot-autoconfigure/               ← Blocking Autoconfigure
│   └── src/main/java/cn/aryee/dict/autoconfigure/
│       └── AryeeDictAutoConfiguration.java
├── dict-spring-boot-starter/                     ← Blocking Starter
├── dict-reactive-spring-boot-autoconfigure/      ← Reactive Autoconfigure
│   └── src/main/java/cn/aryee/dict/reactive/autoconfigure/
│       └── AryeeDictReactiveAutoConfiguration.java
├── dict-reactive-spring-boot-starter/            ← Reactive Starter
└── README.md
```

## 架构设计

### Blocking / Reactive 双模式隔离

| 维度 | Blocking | Reactive |
|------|----------|----------|
| 接口 | `DictService` | `ReactiveDictService` |
| 返回类型 | `T` / `List<T>` / `boolean` | `Mono<T>` / `Flux<T>` / `Mono<Boolean>` |
| InMemory 实现 | `InMemoryDictServiceImpl` | `ReactiveInMemoryDictServiceImpl` |
| Cached 实现 | `CachedDictServiceImpl` | `ReactiveCachedDictServiceImpl` |
| Database 实现 | `DatabaseDictServiceImpl` | `ReactiveDatabaseDictServiceImpl` |
| Autoconfigure | `AryeeDictAutoConfiguration` | `AryeeDictReactiveAutoConfiguration` |
| Starter | `dict-spring-boot-starter` | `dict-reactive-spring-boot-starter` |
| 配置前缀 | `aryee.dict` | `aryee.dict` |

### @DictTranslation 翻译流程

```
Jackson 序列化
  ↓
DictTranslationSerializerModifier（BeanSerializerModifier）
  ↓ 扫描 @DictTranslation 注解字段
  ↓ 为每个注解字段插入 DictLabelPropertyWriter
  ↓
DictLabelPropertyWriter.serializeAsField()
  ↓ 从 bean 反射读取原字段值
  ↓ 调用 DictService.getLabel(dictCode, value)
  ↓ 将标签写入 label 字段
  ↓
JSON 输出（原字段 + label 字段）
```

## 兼容性

| 环境 | 版本要求 |
|------|----------|
| JDK | 21+ |
| Spring Boot | 4.0.7 |
| Jackson | 由 bom-base 统一管理 |
| cache 模块（可选） | 1.0.0-SNAPSHOT |
| database 模块（可选） | 1.0.0-SNAPSHOT |
| tenant 模块（可选） | 1.0.0-SNAPSHOT |
| event 模块（可选） | 1.0.0-SNAPSHOT |
| i18n 模块（可选） | 1.0.0-SNAPSHOT |
