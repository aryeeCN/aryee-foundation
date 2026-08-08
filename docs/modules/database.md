# Aryee Database 数据库基础设施模块

> **所属项目**: [Aryee Foundation](../../README.md)
> **架构层次**: 基础设施层 (Foundation Layer)
> **技术栈**: Java 21, Spring Boot 4.0.7, Spring Cloud 2025.1.2, MyBatis-Plus 3.5.17, R2DBC, JPA
> **访问模式**: 阻塞式 (JDBC / JPA) / 响应式 (R2DBC)

## 简介

数据库基础设施模块基于三层架构（API / Infrastructure / Autoconfigure）实现，提供统一的数据访问抽象层。Blocking 模式集成 MyBatis-Plus 与 JPA，Reactive 模式集成 R2DBC，支持多数据库、连接池管理、分页、多租户、软删除、审计字段、数据加密、数据版本、数据权限等企业级特性。

### 核心特性

- ✅ **三层架构**: API 契约层 + Infrastructure 实现层 + Autoconfigure 自动配置层
- ✅ **双模式隔离**: Blocking（JDBC / JPA）与 Reactive（R2DBC）接口与实现完全隔离，使用独立 Starter
- ✅ **多实现支持**: Blocking 提供 `jdbc`（MyBatis-Plus）和 `jpa`（Hibernate）两种实现；Reactive 提供 `r2dbc` 实现
- ✅ **统一服务接口**: `BaseDataService<T, ID>` / `ReactiveBaseDataService<T, ID>` 屏蔽底层 ORM 差异
- ✅ **统一分页模型**: `PageRequest` / `PageResult` 不耦合 Spring Data Page / MyBatis-Plus IPage
- ✅ **实体基类**: `Entity` / `AuditableEntity` / `SoftDeletable` 提供审计字段与软删除支持
- ✅ **企业级特性**: 多租户、数据加密、数据版本、数据权限、数据审计、SQL 日志、慢 SQL 监控
- ✅ **声明式数据权限**: `@DataPermission` / `@DataPermissionIgnore` 注解 + AOP 切面，自动构建 SQL 过滤条件
- ✅ **动态数据源**: `@DataSource` 注解 + AOP 切面支持方法级数据源切换
- ✅ **Spring Boot 自动配置**: 通过 `AutoConfiguration.imports` 自动装配

## 模块结构

```
aryee-foundation-database/
├── database-api/                               # API 契约层
│   └── cn.aryee.database.api
│       ├── annotation/                         # 注解
│       │   ├── DataSource.java                 # 数据源切换注解
│       │   ├── Deleted.java                    # 软删除字段标记
│       │   ├── CompareField.java
│       │   ├── DataPermission.java             # 声明式数据权限注解
│       │   ├── DataPermissionIgnore.java       # 数据权限忽略注解
│       │   ├── DataPermissionContext.java       # 数据权限上下文（不可变值对象 + SQL 构建）
│       │   ├── DataPermissionContextHolder.java # Blocking 上下文持有器（ThreadLocal）
│       │   └── ReactiveDataPermissionContextHolder.java # Reactive 上下文持有器（Reactor Context）
│       ├── config/                             # 配置属性
│       │   └── DatabaseProperties.java         # 配置前缀 aryee.database
│       ├── constant/PageConstants.java
│       ├── entity/                             # 实体基类
│       │   ├── Entity.java
│       │   ├── AuditableEntity.java
│       │   └── SoftDeletable.java
│       ├── exception/DatabaseException.java
│       ├── model/                              # 数据模型
│       │   ├── PageInfo.java, PageResponse.java
│       │   ├── QueryCondition.java, BaseConditionVO.java
│       │   ├── DataAuditLog.java, ChangeDetailVO.java
│       │   ├── DataPermissionRule.java, DataScope.java, DataScopeType.java
│       │   ├── EncryptFieldConfig.java, EntityVersion.java
│       │   ├── FieldExtend.java, TenantContext.java
│       ├── query/                              # 分页查询
│       │   ├── PageRequest.java
│       │   └── PageResult.java
│       ├── repository/                         # Repository 契约
│       │   ├── Repository.java
│       │   └── SoftDeletableRepository.java
│       ├── service/                            # 服务契约
│       │   ├── BaseDataService.java            # Blocking 主接口
│       │   ├── ReactiveBaseDataService.java    # Reactive 主接口
│       │   ├── BaseDataServiceFactory.java
│       │   ├── ReactiveBaseDataServiceFactory.java
│       │   ├── DataAuditService.java           # 审计/加密/版本/权限/租户 子接口
│       │   ├── DataEncryptionService.java
│       │   ├── DataVersioningService.java
│       │   ├── DataPermissionService.java
│       │   ├── MultiTenancyService.java
│       │   └── Reactive* 对应接口
│       └── util/QueryBuilder.java
│
├── database-infrastructure/                    # 实现层
│   └── cn.aryee.database.infrastructure
│       ├── blocking/                           # Blocking 实现
│       │   ├── jdbc/                           # MyBatis-Plus 实现
│       │   │   ├── aop/DataSourceAspect.java
│       │   │   ├── config/                     # DataSourceConfig / JdbcConfig / DynamicDataSource / AryeeMetaObjectHandler
│       │   │   ├── model/BaseJdbcEntity.java, BaseModel.java
│       │   │   ├── repository/BaseJdbcRepository.java
│       │   │   ├── service/BaseJdbcDataService.java, BaseJdbcDataServiceFactory.java
│       │   │   └── util/JdbcUtil.java
│       │   └── jpa/                            # JPA 实现
│       │       ├── model/BaseJpaEntity.java
│       │       ├── repository/BaseRepository.java
│       │       ├── service/BaseJpaDataService.java, BaseJpaDataServiceFactory.java
│       │       └── util/CompareUtil.java, PageUtil.java
│       └── reactive/                           # Reactive 实现
│           └── r2dbc/                          # R2DBC 实现
│               ├── aop/DataSourceAspect.java
│               ├── config/                     # R2dbcConfig / R2dbcDataSourceConfig / DynamicR2dbcConnectionFactory
│               ├── model/BaseR2dbcEntity.java
│               ├── repository/BaseR2dbcRepository.java
│               ├── service/BaseR2dbcDataService.java, BaseR2dbcDataServiceFactory.java
│               └── util/DataSourceContextHolder.java, R2dbcUtil.java
│
├── database-spring-boot-autoconfigure/         # Blocking 自动配置
│   └── cn.aryee.database.autoconfigure
│       ├── AryeeDatabaseAutoConfiguration.java
│       ├── AryeeJdbcAutoConfiguration.java
│       ├── datapermission/                     # 声明式数据权限
│       │   ├── DataPermissionAspect.java        # Blocking AOP 切面
│       │   ├── DataPermissionUserResolver.java  # 用户信息解析 SPI
│       │   └── SecurityApiUserResolver.java     # security-api 默认实现（反射）
│       └── impl/                               # 默认内存版扩展服务实现
│
├── database-reactive-spring-boot-autoconfigure/ # Reactive 自动配置
│   └── cn.aryee.database.reactive.autoconfigure
│       ├── AryeeDatabaseReactiveAutoConfiguration.java
│       ├── AryeeR2dbcAutoConfiguration.java
│       └── datapermission/                     # 声明式数据权限（Reactive）
│           ├── ReactiveDataPermissionAspect.java
│           ├── ReactiveDataPermissionUserResolver.java
│           └── SecurityApiReactiveUserResolver.java
│
├── database-spring-boot-starter/               # Blocking Starter
└── database-reactive-spring-boot-starter/      # Reactive Starter
```

**自动配置注册**：
- Blocking: `cn.aryee.database.autoconfigure.AryeeDatabaseAutoConfiguration`、`cn.aryee.database.autoconfigure.AryeeJdbcAutoConfiguration`
- Reactive: `cn.aryee.database.reactive.autoconfigure.AryeeDatabaseReactiveAutoConfiguration`、`cn.aryee.database.reactive.autoconfigure.AryeeR2dbcAutoConfiguration`

## 使用方法

### 1. 引入 BOM（推荐）

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

### 2. 按需引入 Starter

```xml
<!-- 阻塞式（MyBatis-Plus / JPA 场景） -->
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>database-spring-boot-starter</artifactId>
</dependency>

<!-- 或：响应式（R2DBC 场景，二选一，禁止同时引入） -->
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>database-reactive-spring-boot-starter</artifactId>
</dependency>
```

## 配置

配置前缀：`aryee.database`

```yaml
aryee:
  database:
    # 是否启用数据库模块
    enabled: true
    # 数据库类型：mysql / postgresql / sqlite / oracle / sqlserver
    type: mysql
    # 数据库连接（也可通过 spring.datasource / spring.r2dbc 配置）
    url: jdbc:mysql://localhost:3306/aryee_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver

    # 连接池配置
    pool:
      initial-size: 5
      min-idle: 5
      max-active: 20
      max-wait: 60000
      max-lifetime: 1800000
      idle-timeout: 600000
      connection-timeout: 20000
      validation-timeout: 5000
      test-on-borrow: false
      test-while-idle: true
      test-on-return: false
      time-between-eviction-runs-millis: 60000
      name: aryee-db-pool

    # 多租户配置
    multi-tenant:
      enabled: false
      mode: schema             # schema / database / column
      tenant-id-column: tenant_id
      default-tenant-id: default
      strategy: row_level      # row_level / schema / database

    # SQL 日志
    sql-log:
      enabled: false
      show-params: true
      show-execute-time: true
      slow-query-threshold: 3000   # 慢 SQL 阈值（毫秒）

    # 审计
    audit:
      enabled: false

    # 数据权限
    permission:
      enabled: false

    # 数据加密
    encryption:
      enabled: false
      fields:
        - field-name: idCard
          algorithm: AES
          key-ref: aes-key-ref
          encrypt-on-write: true
          decrypt-on-read: true

    # 数据版本
    versioning:
      enabled: false
```

## 代码示例

### Blocking 模式（MyBatis-Plus / JPA）

```java
// 1. 定义实体（继承 AuditableEntity，实现 SoftDeletable）
@Data
@TableName("sys_user")
public class User extends AuditableEntity<Long> implements SoftDeletable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String email;
    private Integer status;

    @Deleted
    private Integer deleted;
}

// 2. 使用 BaseDataService（推荐，屏蔽底层 ORM 差异）
@Service
public class UserService {

    private final BaseDataService<User, Long> userService;

    public UserService(BaseDataService<User, Long> userService) {
        this.userService = userService;
    }

    public User createUser(User user) {
        user.setStatus(1);
        return userService.create(user);
    }

    public User getUserById(Long id) {
        return userService.getByIdOrThrow(id);
    }

    // 统一分页（返回 PageResult，不耦合 Spring Data Page / IPage）
    public PageResult<User> listUsers(int pageNum, int pageSize) {
        return userService.getPage(pageNum, pageSize);
    }

    // 条件分页
    public PageResult<User> searchUsers(Map<String, Object> condition, int pageNum, int pageSize) {
        return userService.getPageByCondition(condition, pageNum, pageSize);
    }

    // 软删除
    public void deleteUser(Long id) {
        userService.softDeleteById(id);
    }

    // 批量操作
    public List<User> batchCreate(List<User> users) {
        return userService.batchCreate(users);
    }

    // 字段查询
    public Optional<User> findByUsername(String username) {
        return userService.getOneByField("username", username);
    }
}
```

### Reactive 模式（R2DBC）

```java
@Service
public class ReactiveUserService {

    private final ReactiveBaseDataService<User, Long> userService;

    public ReactiveUserService(ReactiveBaseDataService<User, Long> userService) {
        this.userService = userService;
    }

    public Mono<User> createUser(User user) {
        user.setStatus(1);
        return userService.create(user);
    }

    public Mono<User> getUserById(Long id) {
        return userService.getByIdOrThrow(id);
    }

    public Flux<User> listAllUsers() {
        return userService.getAll();
    }

    public Mono<PageResult<User>> listUsers(int pageNum, int pageSize) {
        return userService.getPage(pageNum, pageSize);
    }

    public Mono<Void> deleteUser(Long id) {
        return userService.softDeleteById(id);
    }

    public Flux<User> findByStatus(Integer status) {
        return userService.getByField("status", status);
    }
}
```

### 动态数据源切换

```java
@Service
public class OrderService {

    // 方法级数据源切换（通过 @DataSource 注解 + AOP 切面）
    @DataSource("master")
    public Order writeOrder(Order order) {
        return orderService.create(order);
    }

    @DataSource("slave")
    public Order readOrder(Long id) {
        return orderService.getByIdOrThrow(id);
    }
}
```

## 安全管控

数据库模块支持可选的安全审计能力，委托 [security 模块](security.md) 的 `SecurityAuditService` 进行权限变更审计，遵循 [security-governance.md](https://github.com/aryeecn/aryee-foundation)（内部规范：security-governance） 规则。

### 工作原理

```
调用方 → DataPermissionService.addPermissionRule() / updatePermissionRule() / deletePermissionRule()
              ↓
     recordPermissionChange()
              ↓
     委托 SecurityAuditService.logDataAccess()  （委托 security 模块）
```

### 安全风险等级

| 操作 | 风险等级 | 审计行为 |
|------|---------|---------|
| `addPermissionRule()` | 🔴 高 | 记录权限规则新增审计 |
| `updatePermissionRule()` | 🔴 高 | 记录权限规则变更审计 |
| `deletePermissionRule()` | 🔴 高 | 记录权限规则删除审计 |

### 配置示例

```yaml
aryee:
  database:
    permission:
      enabled: true       # 启用数据权限服务
    audit:
      enabled: true       # 启用数据审计服务
```

### 条件装配

| Bean | 条件 | 说明 |
|------|------|------|
| `InMemoryDataPermissionService(SecurityAuditService)` | `SecurityAuditService` Bean 存在 | 审计委托 security 模块 |
| `InMemoryDataPermissionService()` | `SecurityAuditService` Bean 不存在 | 降级为本地日志输出 |

## 声明式数据权限（@DataPermission）

基于 AOP 切面 + ThreadLocal/Reactor Context 实现的声明式数据权限，无需手动拼接 SQL，注解即用。

### 核心组件

| 组件 | 模式 | 职责 |
|------|------|------|
| `@DataPermission` | 通用 | 声明数据权限规则（resourceCode/scope/deptField/userField） |
| `@DataPermissionIgnore` | 通用 | 忽略数据权限（管理员接口/定时任务） |
| `DataPermissionContext` | 通用 | 不可变值对象，构建 SQL 过滤片段 |
| `DataPermissionContextHolder` | Blocking | ThreadLocal 持有器，跨线程通过 capture/restore 传递 |
| `ReactiveDataPermissionContextHolder` | Reactive | Reactor Context 持有器，自动跨线程传递 |
| `DataPermissionAspect` | Blocking | AOP 切面，写入 ThreadLocal |
| `ReactiveDataPermissionAspect` | Reactive | AOP 切面，写入 Reactor Context |
| `DataPermissionUserResolver` | Blocking | 用户信息解析 SPI（业务可覆盖） |
| `ReactiveDataPermissionUserResolver` | Reactive | 用户信息解析 SPI（业务可覆盖） |

### 数据范围类型

| `DataScopeType` | SQL 过滤条件 |
|-----------------|-------------|
| `ALL` | 无过滤 |
| `SELF` | `user_field = 'userId'` |
| `DEPT` | `dept_field IN ('deptId')` |
| `DEPT_AND_CHILD` | `dept_field IN ('deptId', 'childDeptIds...')` |
| `SPECIFIED_DEPT` | `dept_field IN ('customDeptIds')` |
| `CUSTOM` | 由 `DataPermissionService.buildSqlFilter` 提供 |
| `SAME_LEVEL` | 业务层自行扩展 |

### Blocking 使用示例

```java
@Service
public class OrderService {

    // 仅查询本人订单
    @DataPermission(resourceCode = "order", scope = DataScopeType.SELF)
    public List<Order> listMyOrders(OrderQuery query) {
        return orderRepository.findAll(query);
    }

    // 查询本部门及下级部门订单
    @DataPermission(resourceCode = "order", scope = DataScopeType.DEPT_AND_CHILD,
                   deptField = "dept_id", userField = "creator")
    public List<Order> listDeptOrders(OrderQuery query) {
        return orderRepository.findAll(query);
    }

    // 管理员后台导出，不进行数据权限过滤
    @DataPermissionIgnore(reason = "admin export")
    public List<Order> exportAllOrders(OrderQuery query) {
        return orderRepository.findAll(query);
    }
}

// Repository 层读取上下文构建 SQL
@Component
public class OrderRepository extends BaseJdbcRepository<Order, Long> {

    public List<Order> findAll(OrderQuery query) {
        DataPermissionContext ctx = DataPermissionContextHolder.get();
        if (ctx != null && ctx.isValid()) {
            String filter = ctx.buildSqlFilter("t1");
            if (!filter.isEmpty()) {
                query.appendWhere(filter);
            }
        }
        return super.findAll(query);
    }
}
```

### Reactive 使用示例

```java
@Service
public class ReactiveOrderService {

    @DataPermission(resourceCode = "order", scope = DataScopeType.SELF)
    public Flux<Order> listMyOrders(OrderQuery query) {
        return orderRepository.findAll(query);
    }
}

// Reactive Repository 从 Reactor Context 读取
@Component
public class ReactiveOrderRepository {

    public Flux<Order> findAll(OrderQuery query) {
        return Mono.deferContextual(ctx -> {
            DataPermissionContext dpCtx = ReactiveDataPermissionContextHolder.fromContext(ctx);
            if (dpCtx != null && dpCtx.isValid()) {
                String filter = dpCtx.buildSqlFilter("t1");
                if (!filter.isEmpty()) {
                    query.appendWhere(filter);
                }
            }
            return databaseClient.sql(query.toSql())
                    .bindAll(query.getParams())
                    .map(row -> rowToOrder(row))
                    .all();
        });
    }
}
```

### 用户上下文 SPI

默认通过反射调用 `security-api` 的 `SecurityContextHolder`，业务项目可自定义：

```java
@Component
public class BusinessUserResolver implements DataPermissionUserResolver {

    @Override
    public String resolveUserId() {
        return RequestContextHolder.currentRequestAttributes()
                .getAttribute("userId", RequestAttributes.SCOPE_REQUEST).toString();
    }

    @Override
    public Set<String> resolveDeptIds() {
        // 加载当前用户部门及下级部门
        return deptService.loadUserDeptTree(resolveUserId());
    }
}
```

### 与 DataPermissionService 联动

当 `aryee.database.permission.enabled=true` 且容器中存在 `DataPermissionService` Bean 时，
切面会调用 `DataPermissionService.getDataScope(userId, resourceCode)` 动态计算 scope，
并使用 `DataPermissionService.buildSqlFilter(resourceCode, userId)` 作为 `CUSTOM` 范围的 SQL 条件。

```yaml
aryee:
  database:
    permission:
      enabled: true       # 启用 DataPermissionService，支持动态规则
```

## 兼容性

| 数据库 | Blocking (JDBC) | Blocking (JPA) | Reactive (R2DBC) |
|--------|-----------------|----------------|-------------------|
| MySQL 8.x | ✅ | ✅ | ✅ |
| PostgreSQL 14+ | ✅ | ✅ | ✅ |
| SQLite | ✅ | ✅ | ⚠️ 有限支持 |
| Oracle | ✅ | ✅ | ⚠️ 有限支持 |
| SQL Server | ✅ | ✅ | ⚠️ 有限支持 |

| 环境 | 版本要求 |
|------|----------|
| JDK | 21+ |
| Spring Boot | 4.0.7 |
| Spring Cloud | 2025.1.2 |
| MyBatis-Plus | 3.5.17+ |
| R2DBC | 1.0+ |
| JPA (Hibernate) | 6.x |

### Blocking vs Reactive 选型

- **Blocking 场景**: 传统同步业务、复杂事务管理、MyBatis-Plus 生态、JPA 实体管理（管理后台、ERP、CRM）
- **Reactive 场景**: 高并发非阻塞 I/O、WebFlux 技术栈、响应式全链路（网关、实时流处理）

> **重要**: Blocking 与 Reactive Starter 必须二选一，禁止同时引入。JDBC 与 JPA 同属 Blocking，可在同一应用中并存或择一使用。
