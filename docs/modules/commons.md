# Aryee Foundation Commons 公共基础模块

> **所属项目**: [Aryee Foundation](../../README.md)
> **架构层次**: 公共基础（所有功能模块的依赖基础）
> **技术栈**: Java 21 + Lombok 1.18.42 + Spring Framework 7.0.x（仅 commons-spring/commons-web）

## 简介

Aryee Foundation Commons 是整个项目的公共基石，提供跨模块复用的「工具类 / 领域基类 / 异常体系 / 响应协议 / 枚举规范 / 上下文 / ID 生成 / Spring 扩展 / Web 通用组件」等通用能力。

设计原则：**零业务侵入、按依赖强度分子模块、commons-core 纯 JDK 零 Spring 依赖**。所有模块 groupId 为 `cn.aryee.foundation`。

### 核心特性

- ✅ **分层依赖**: 4 个子模块按依赖强度递增，按需引入避免依赖污染
- ✅ **零 Spring 依赖**: commons-core 纯 JDK 实现，可在任何环境使用
- ✅ **统一异常体系**: `GlobalException` 唯一基础异常根类，禁止 `BaseException`
- ✅ **统一响应协议**: `R<T>` 标准响应格式 + `PageResult<T>` 分页结果
- ✅ **统一枚举规范**: 所有枚举实现 `EnumService<T>` 接口，配合 `EnumUtil` 获得 O(1) 缓存查找
- ✅ **雪花 ID 生成**: 纯 JDK 实现 + Spring 托管版本
- ✅ **70+ 工具类**: 全部 null 安全，按职责分子包

## 模块结构

```
aryee-foundation-commons/             # Commons 聚合 POM
├── commons-core/                     # 核心基础（纯 JDK，零 Spring 依赖）
│   └── src/main/java/cn/aryee/commons/
├── commons-domain/                   # 通用领域模型（依赖 commons-core）
│   └── src/main/java/cn/aryee/commons/domain/
├── commons-spring/                   # Spring 扩展工具（依赖 Spring + commons-core/domain）
│   └── src/main/java/cn/aryee/commons/spring/
└── commons-web/                      # Web 层工具（依赖 Spring-Web + commons-spring）
    └── src/main/java/cn/aryee/commons/web/
```

### 各模块依赖定位

| 模块 | artifactId | 依赖 Spring？ | 典型使用场景 |
|---|---|---|---|
| **commons-core** | `commons-core` | ❌ 纯 JDK 21 | 任何地方都能用：枚举服务、异常、响应 R、工具类、ID 生成、I18N、上下文 |
| **commons-domain** | `commons-domain` | ❌ 纯 JDK + Lombok | DDD 分层：实体基类、分页、查询模型、规格模式、值对象、领域事件 |
| **commons-spring** | `commons-spring` | ✅ Spring Framework 7.0.x | Spring 容器内：Bean 拷贝、JSON、SpEL、AOP、Spring 雪花 ID、动态任务 |
| **commons-web** | `commons-web` | ✅ Spring-Web | WebMVC/WebFlux：全局异常、响应包装、过滤器、Web 日志切面、SQL 注入防御 |

> **依赖方向**：`commons-web → commons-spring → commons-domain → commons-core`，禁止反向依赖。

## 使用方法

### Maven 依赖

```xml
<!-- 根 pom 先引入 BOM，无需写版本 -->
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

<!-- 子模块按需引入（不指定 version，由 BOM 统一管理） -->
<dependencies>
    <dependency>
        <groupId>cn.aryee.foundation</groupId>
        <artifactId>commons-core</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.aryee.foundation</groupId>
        <artifactId>commons-domain</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.aryee.foundation</groupId>
        <artifactId>commons-spring</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.aryee.foundation</groupId>
        <artifactId>commons-web</artifactId>
    </dependency>
</dependencies>
```

### 配置选项

commons-spring 提供线程池自动配置（前缀 `aryee.thread-pool`）：

```yaml
aryee:
  thread-pool:
    enabled: true
    core-pool-size: 8
    max-pool-size: 64
    queue-capacity: 256
    thread-name-prefix: aryee-
```

### 代码示例

```java
// ===== 统一响应 + 异常抛出 =====
public R<UserVO> getUser(Long id) {
    User user = userRepository.findById(id);
    if (user == null) {
        GlobalException.throwError(ErrorCodeEnum.DATA_NOT_FOUND, "用户不存在: " + id);
    }
    return R.ok(new UserVO().copyFrom(user));
}

// ===== 枚举规范（实现 EnumService<T>）=====
public enum OrderStatusEnum implements EnumService<Integer> {
    PENDING(1, "待支付"),
    PAID(2, "已支付");

    private final Integer key;
    private final String description;
    // ... 构造器 / getter
}

// 通过 EnumUtil 查找（带缓存）
OrderStatusEnum status = EnumUtil.byKey(OrderStatusEnum.class, 1);

// ===== 雪花 ID 生成（纯 JDK）=====
long id = SnowflakeIdUtil.nextId();

// ===== Spring 环境的 Bean 转换 =====
List<UserVO> voList = BeanConvertUtil.convertList(userEntityList, UserVO.class);

// ===== JSON 路径读写 =====
String phone = JsonUtil.getByPath(jsonStr, "$.user.contact.phone");
```

## 1️⃣ commons-core 核心基础

### 包结构

```
cn.aryee.commons
├── annotation/          @Traced 链路追踪注解 / @NonNull / @Nullable
├── constant/            全局常量（按子包分类，禁止硬编码散落）
│   ├── datetime/        DateTimeConstants
│   ├── regex/           RegexConstants
│   ├── status/          Auth/Common/Data/Network/Process/Resource/Service/Session/Task/User 10 组状态
│   ├── symbol/          SymbolConstants（COLON / COMMA / DOT / SLASH 等）
│   └── tracing/         TracingConstants
├── context/             ServiceContext（业务上下文）、TraceContext（链路追踪上下文）
├── enums/               通用枚举（全部实现 EnumService<T> 接口）
│   ├── CommonStatusEnum / HttpStatusEnum / YesNoEnum / StatusEnum / DataFormatEnum
│   ├── ErrorCodeEnum              统一错误码（1000+）
│   ├── ServiceErrorCodeRangeEnum  模块错误码范围分配表
│   ├── KeyFormatEnum / DateIntervalEnum / SequenceRefreshTypeEnum
│   └── meta/TransactionPropagationEnum
├── exception/           GlobalException 异常体系
│   ├── GlobalException              唯一顶层基础异常（实现 ErrorCode）
│   ├── GlobalCheckedException       受检异常版本（对称设计）
│   ├── BusinessException / SystemException / ServiceException
│   ├── AuthException / AuthorizationException / ResourceNotFoundException
├── function/            ToBigDecimalFunction 函数式接口扩展
├── response/            R<T>（唯一推荐响应格式，含 traceId 自动注入）
├── service/             基础服务接口契约
│   ├── FoundationService<T>  / CoreService<T>  / ResponseService<T>
│   ├── ErrorCode / EnumService / AdapterService<S,T> / ServiceLookup<T,F>
├── support/             辅助类
│   ├── id/              IdGenerator 接口 + SnowflakeIdGenerator 纯 JDK 雪花算法
│   ├── AuthResponse / CapacityLimitedStorage / InMemoryAuditLogStorage
│   ├── DataTypeDefinition / HtmlFilter / Pair / Triple / ValidationError
└── util/                工具类（按子包细分，全部 null 安全）
    ├── codec/           Base32Util / Base64Util / HexUtil / UrlCodecUtil
    ├── collection/      ArrayUtil / CollectionUtil / CollectionConvertUtil
    │                    CollectorsUtil / StreamCollectorsUtil / ForEachUtil
    ├── concurrency/     ConcurrencyUtil
    ├── convert/         ConvertUtil / BasicTypeConvertUtil / ArrayTypeConvertUtil
    │                    BeanCopyUtil / ChineseNumberUtil / ExtendedConvertUtil
    │                    SpecialConvertUtil / StrArrayConvertUtil
    ├── crypto/          AesUtil / CrcUtil / Md5Util / PasswordUtil / RsaUtil / ShaUtil / TotpUtil
    ├── data/            CharsetUtil / FilePathUtil / FileUtil / IpUtil
    │                    SnowflakeIdUtil / SnowflakeIdValidateUtil
    ├── date/            DateUtil（JDK8+ DateTime API 封装）
    ├── geo/             GeoUtil（Haversine 距离、围栏、bbox 粗筛）
    ├── i18n/            I18nUtil / NationUtil / PinyinUtil
    ├── io/              IoUtil / ResourceUtil / StreamUtil
    ├── lang/            ClassUtil / ReflectUtil
    ├── network/         HttpUtil
    ├── number/          BitUtil / ByteFormatUtil / MoneyUtil / NumberUtil / RandomUtil
    ├── string/          StringUtil / NamingUtil / RegexUtil / EscapeUtil / UrlUtil
    │                    StringConvertUtil / StringFormatUtil / StringRandomUtil
    ├── system/          AssertUtil / AsyncUtil / DesensitizeUtil / DiffUtil / EnumUtil
    │                    ObjectUtil / RetryUtil / SafeCastUtil / SequenceFormatUtil
    │                    SystemUtil / ThreadUtil / TypeCheckUtil / ValidationUtil / VersionUtil
    └── tree/            TreeUtil（树构建/查找/路径/过滤/遍历）
```

### 核心能力速查

| 能力 | 入口 | 说明 |
|---|---|---|
| 统一响应 | `R.ok()` / `R.fail()` | code/msg/data/timestamp/traceId/extra |
| 异常抛出 | `GlobalException.throwError(code, msg)` | 绑定 ErrorCode，自动 i18n |
| 枚举查找 | `EnumUtil.byKey()` / `EnumUtil.by()` | 带 ClassValue 缓存加速 |
| 雪花 ID | `SnowflakeIdUtil.nextId()` / `SnowflakeIdUtil.nextIdStr()` | 纯 JDK，无需 Spring |
| 上下文 | `ServiceContext` / `TraceContext` | ThreadLocal 实现，需 Filter 清理 |
| 脱敏 | `DesensitizeUtil.mobilePhone()` 等 | 手机号/身份证/银行卡/姓名/邮箱/地址/车牌/IP |
| 树结构 | `TreeUtil.buildTree()` / `findPath()` | 部门/分类/地区树 |

## 2️⃣ commons-domain 通用领域模型

### 包结构

```
cn.aryee.commons.domain
├── event/               领域事件
│   ├── DomainEvent                  领域事件基类
│   ├── DomainEventPublisher         事件发布器接口
│   └── SimpleDomainEventBus         简单内存事件总线实现
├── model/               领域模型
│   ├── BaseAggregateRoot            聚合根基类（继承 BaseEntity）
│   ├── BaseEntity                   持久化实体基类（createTime/updateTime/creator/updater/deleted/version/remark/ext）
│   ├── DTOModel<T>                  DTO 入参基类（递归泛型 + 链式调用 + convertTo）
│   ├── VOModel<T>                   VO 出参基类（与 DTOModel 对称 + copyFrom）
│   ├── QueryModel                   分页查询入参基类（pageNum/pageSize/orderBy/queryAll/offset/limit）
│   ├── PageResult<T>                统一分页出参（records/total/size/current/pages/hasNext/hasPrevious）
│   ├── TreeModel                    树节点基类（parentId/sort/children）
│   └── TreeVO                       树节点出参基类
├── specification/       DDD 规格模式
│   ├── Specification<T>             规格接口（isSatisfiedBy + and/or/not 组合）
│   └── Specifications               规格工厂（静态构造方法）
└── valueobject/         值对象
    ├── Email / IdCard / PhoneNumber    联系信息值对象
    ├── Identifier / Money / Password   通用值对象
    ├── TimePeriod                       时间段（含 Allen 区间代数关系判断）
    └── Url                              URL 值对象
```

### 使用示例

```java
// 实体继承 BaseEntity（自动获得审计字段）
@Data
@EqualsAndHashCode(callSuper = true)
public class UserEntity extends BaseEntity {
    private String username;
    private String phone;
}

// 保存前填充审计信息
entity.initCreateInfo(currentUserId);
entity.updateInfo(currentUserId);
entity.markAsDeleted();

// DTO/VO 链式调用 + 类型转换
UserQueryDTO query = new UserQueryDTO().setUsername("alice").setStatus(1);
UserEntity entity = query.convertTo(UserEntity.class);
UserVO vo = new UserVO().copyFrom(entity);

// 分页查询返回
PageResult<UserVO> result = PageResult.of(voList, total, pageSize, pageNum);
```

## 3️⃣ commons-spring Spring 扩展工具

### 包结构

```
cn.aryee.commons.spring
├── config/              自动配置
│   ├── AryeeThreadPoolConfiguration   线程池自动装配
│   └── AryeeThreadPoolProperties      线程池配置属性（aryee.thread-pool）
├── support/             Spring 扩展支持
│   ├── bean/DynamicBeanRegistrar                 动态 Bean 注册器
│   ├── event/AbstractSpringContextEventListener  Spring 上下文事件监听基类
│   ├── event/SpringDomainEventPublisher          领域事件发布器（基于 Spring ApplicationEventPublisher）
│   ├── id/SpringSnowflakeIdGenerator             Spring 托管雪花 ID 生成器
│   └── scheduled/DynamicTaskRegistrar            动态定时任务注册器
└── util/                工具类
    ├── aop/AopUtil                                AOP 辅助（切点解析/代理判断）
    ├── concurrent/ThreadPoolMonitor               线程池监控（Metrics / HealthStatus）
    ├── concurrent/ThreadPoolMetrics
    ├── concurrent/ThreadPoolHealthStatus
    ├── config/ConfigUtil                          配置读取辅助
    ├── convert/BeanUtil                           Bean 拷贝（基于 Spring BeanUtils，忽略 null）
    ├── convert/BeanConvertUtil                    类型自适应转换（Date↔Long↔String）
    ├── data/ByteUtil                              字节转换 + 序列化
    ├── data/YamlUtil                              YAML 读写（基于 SnakeYAML）
    ├── json/JsonUtil                              Jackson 深度封装（路径读写）
    ├── json/JsonObjectMapperUtil                  全局 ObjectMapper 配置
    ├── json/JacksonCustomizerUtil                 Jackson 定制器集合
    ├── system/SpringUtil                          Spring 上下文工具（getBean/publishEvent）
    ├── system/EnvUtil                             环境变量工具
    ├── system/SpelExpressionUtil                  SpEL 表达式求值
    ├── system/SpelKeyGenerator                    SpEL 缓存 Key 生成
    ├── system/SpringAsyncUtil                     Spring 异步任务工具
    ├── system/TransactionUtil                     Spring 事务工具（基于 spring-tx）
    └── ReactiveHttpUtil                           响应式 HTTP 工具（基于 WebClient，可选）
```

## 4️⃣ commons-web Web 层工具

### 包结构

```
cn.aryee.commons.web
├── advice/              Controller 增强
│   ├── GlobalExceptionHandler         全局异常处理（@ControllerAdvice，转 R 响应）
│   └── ResponseBodyWrapAdvice         响应体包装（自动包装非 R 返回值）
├── annotation/          Web 注解
│   └── Phone                          手机号校验注解
├── aspect/              切面
│   └── WebLogAspect                   Web 访问日志切面
├── constant/            Web 常量
│   └── HeaderConstants                请求头常量
├── filter/              过滤器
│   ├── RepeatableReadFilter           请求体可重复读过滤器
│   ├── ReplayCheckFilter              重放校验过滤器
│   ├── RequestIdFilter                请求 ID 注入过滤器
│   ├── SecureResponseHeaderFilter     安全响应头过滤器
│   └── XssFilter                      XSS 过滤器
├── support/             Web 辅助
│   └── ApiResponseUtil                响应工具
└── util/                Web 工具
    ├── FileUploadUtil                 文件上传工具
    ├── RateLimitUtil                  限流工具
    └── SqlInjectionUtil               SQL 注入检测工具
```

## 兼容性

| 依赖 | 最低版本 | 说明 |
|---|---|---|
| **JDK** | 21 | 使用 `java.time`、`@Serial`、模式匹配等新 API |
| **Spring Boot** | 4.0.6 | Jakarta EE 11 + Spring Framework 7.0.x |
| **Lombok** | 1.18.42 | commons-domain 依赖（@Data / @Accessors 等） |
| **Jackson** | 2.22.1 | commons-spring 的 JSON 工具依赖（BOM 已管理） |
| **Hibernate Validator** | 8.x | `ValidationUtil` 可选依赖（不引入则相关方法抛友好提示） |
| **SnakeYAML** | 2.3 | commons-spring 的 `YamlUtil` 依赖 |

## 设计约束

| 约束 | 说明 |
|---|---|
| **泛型无 Raw Type** | 所有 `EnumService<T>` / `List<E>` 使用必须参数化，禁止裸类型 |
| **工具类规范** | 纯工具类必须在 `util/` 目录并以 `Util` 结尾；上下文/注解/Bean 实现移出 util |
| **异常继承** | 禁止定义 `BaseException`；所有自定义异常必须继承 `GlobalException` 并绑定错误码枚举 |
| **方法引用空安全** | 泛型接口方法引用统一用 Lambda `e -> e.xxx()`，避免 JDT Null type safety 警告 |
| **commons-core 零业务** | commons-core 禁止出现任何业务枚举/DTO/常量，禁止 import `org.springframework.*` |

### 异常处理规范

#### 强制规则

1. **所有基础设施级异常必须使用 `GlobalException` 子类**，禁止直接抛出 `RuntimeException`。允许的异常层级如下：

```
GlobalException（唯一顶层基础异常，实现 ErrorCode 接口）
├── BusinessException          业务逻辑异常（如参数校验、业务规则不满足）
├── SystemException            系统级异常（如配置错误、资源不足）
├── ServiceException           服务调用异常（如远程调用失败）
├── AuthException              认证异常（如 Token 过期、未登录）
├── AuthorizationException     授权异常（如权限不足）
└── ResourceNotFoundException  资源未找到异常
```

2. **所有 `catch` 块必须至少以 debug 级别记录日志**，禁止静默吞噬异常（如 `catch (Exception e) {}`）。

```java
// ❌ 禁止：静默捕获
try {
    parse(input);
} catch (Exception e) {
    // 什么都不做，异常信息丢失
}

// ❌ 禁止：仅打印堆栈
try {
    parse(input);
} catch (Exception e) {
    e.printStackTrace(); // 不应出现在生产代码中
}

// ✅ 正确：至少记录 debug 日志
try {
    parse(input);
} catch (Exception e) {
    log.debug("解析输入失败, input={}", input, e);
    throw GlobalException.throwError(ErrorCodeEnum.PARSE_ERROR, "数据解析失败", e);
}
```

3. **`catch` 子句应尽可能缩小到具体异常类型**，避免使用过于宽泛的 `Exception` 或 `RuntimeException`。

```java
// ❌ 禁止：捕获过于宽泛的异常
try {
    return Long.parseLong(str);
} catch (Exception e) {
    throw new SystemException("转换失败", e);
}

// ✅ 正确：捕获具体异常类型
try {
    return Long.parseLong(str);
} catch (NumberFormatException e) {
    throw GlobalException.throwError(ErrorCodeEnum.PARSE_ERROR, "无效的数字格式: " + str, e);
}
```

#### 设计原则

- **异常即契约**：抛出异常是方法对调用方的契约承诺，必须绑定明确的错误码（`ErrorCode`）
- **语义清晰**：优先选择语义最精确的异常类型，让调用方能区分业务异常与系统异常
- **上下文保留**：包装原始异常时必须通过构造函数保留 `cause`，避免丢失原始堆栈
- **日志与异常分离**：抛出 `GlobalException` 的同时可以记录日志，但禁止只记录日志而不抛出异常（除非明确需要降级处理）

---

**作者**: Aryee Foundation Team
**版本**: 1.0.0-SNAPSHOT
