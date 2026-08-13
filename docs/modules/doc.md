# Aryee Foundation Doc

API 治理与文档平台，基于 SpringDoc OpenAPI 3 + Knife4j 提供开箱即用的接口文档能力。支持所有架构类型（单体/微服务/云原生），网关聚合仅在检测到 Spring Cloud Gateway 时自动启用。

## 核心特性

- **开箱即用**：引入 Starter 即自动装配 OpenAPI + GroupedOpenApi，无需任何配置
- **全架构支持**：单体、微服务、云原生均可使用，网关聚合为条件激活的增值能力
- **统一 Bearer 鉴权**：自动注入 `bearerAuth` SecurityScheme，所有接口默认要求 Token
- **R&lt;T&gt; 响应 Schema**：文档 UI 自动展示完整的统一响应结构示例
- **全局参数注入**：自动向所有接口注入 X-Trace-Id、X-Tenant-Id、Accept-Language 等框架级 Header
- **接口可见性治理**：通过 `@DocHidden` / `@DocVisible` 注解控制接口在文档中的显隐
- **数据脱敏**：通过 `@DocSensitive` 注解在文档中自动脱敏敏感字段
- **离线文档导出**：支持导出 HTML / Markdown / JSON 格式的离线文档
- **网关文档聚合**：Spring Cloud Gateway 环境自动发现下游服务，聚合展示 API 文档（通过 DiscoveryClient 过滤不可用服务）
- **Knife4j 增强 UI**：默认启用 Knife4j，提供更友好的中文 UI（`/doc.html`）
- **双模式支持**：Blocking（WebMVC）与 Reactive（WebFlux）独立 Starter，二选一引入
- **启动日志**：应用启动时打印 Swagger UI / API Docs / Knife4j UI 访问地址
- **微服务增强版**：提供 `apidocmicroservice-spring-boot-starter` 和 `apidocreactive-microservice-spring-boot-starter`，内置 Spring Cloud Commons + Gateway Server，适用于微服务网关文档聚合

## 模块结构

```
aryee-foundation-apidoc/
├── pom.xml                                      # 聚合POM
├── apidocapi/                                     # API 契约层
│   └── src/main/java/cn/aryee/doc/api/
│       ├── annotation/                          # 注解（@DocHidden/@DocVisible/@DocSensitive）
│       ├── config/                              # 配置属性（DocProperties/DocReactiveProperties）
│       ├── constant/                            # 常量（DocConstants）
│       ├── enums/                               # 错误码枚举
│       ├── exception/                           # 异常（DocException）
│       ├── model/                               # 模型（SwaggerResource/GlobalParameter）
│       └── service/                             # 服务接口（DocExportService/DocAggregationService）
├── apidocinfrastructure/                          # 基础设施层
│   └── src/main/java/cn/aryee/doc/infrastructure/
│       ├── customizer/                          # OpenAPI 定制器（响应Schema/全局参数/可见性过滤）
│       ├── export/                              # 离线文档导出实现
│       └── gateway/                             # 网关文档聚合实现
├── apidocspring-boot-autoconfigure/               # Blocking 自动配置
│   └── src/main/java/cn/aryee/doc/autoconfigure/
│       └── AryeeDocAutoConfiguration.java       # 自动配置类
├── apidocreactive-spring-boot-autoconfigure/      # Reactive 自动配置
│   └── src/main/java/cn/aryee/doc/reactive/autoconfigure/
│       └── AryeeDocReactiveAutoConfiguration.java
├── apidocspring-boot-starter/                     # Blocking Starter（依赖聚合）
│   └── pom.xml
└── apidocreactive-spring-boot-starter/            # Reactive Starter（依赖聚合）
    └── pom.xml
```

## 引入方式

### Blocking（WebMVC）

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>apidocspring-boot-starter</artifactId>
</dependency>
```

### Reactive（WebFlux）

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>apidocreactive-spring-boot-starter</artifactId>
</dependency>
```

> ⚠️ Blocking 与 Reactive Starter 必须二选一引入，禁止同时引入。

## 配置项

### Blocking 配置（`aryee.doc.*`）

```yaml
aryee:
  doc:
    enabled: true                          # 是否启用，默认 true
    title: "Aryee Foundation API"          # 文档标题
    description: "Aryee Foundation 接口文档" # 文档描述
    version: "1.0.0"                       # 文档版本
    base-package: "cn.aryee"               # 扫描基础包（多个用逗号分隔）
    # packages-to-scan:                    # 扫描包列表（与 base-package 二选一）
    #   - cn.aryee.user.api
    #   - cn.aryee.order.api
    # paths-to-exclude:                    # 排除路径（Ant 风格）
    #   - /internal/**
    bearer-enabled: true                   # 是否启用 Bearer Token 鉴权
    bearer-name: "Bearer Token"
    knife4j-enabled: true                  # 是否启用 Knife4j 增强 UI
    cors-enabled: true                     # 是否启用 CORS
    default-produces: "application/json"
    contact:
      name: "Aryee"
      url: "https://github.com/aryeecn/aryee-foundation"
      email: "dev@aryee.cn"
    license:
      name: "Apache 2.0"
      url: "https://www.apache.org/licenses/LICENSE-2.0"
    # 全局参数配置
    global-params:
      enabled: true                        # 是否启用全局参数注入
      trace-id: true                       # 注入 X-Trace-Id
      tenant-id: false                     # 注入 X-Tenant-Id
      accept-language: true                # 注入 Accept-Language
      # custom:                            # 自定义全局参数
      #   - name: X-Custom-Header
      #     description: 自定义Header
      #     in: header
      #     required: false
      #     example: "custom-value"
    # 接口可见性配置
    visibility:
      enabled: false                       # 是否启用可见性过滤
      role: "all"                          # 当前文档角色
    # 网关文档聚合（仅 Spring Cloud Gateway 环境生效）
    gateway:
      enabled: false                       # 是否启用网关聚合
      # exclude-services:                  # 排除的服务
      #   - excluded-service
      # services:                          # 手动追加的服务
      #   - name: external-service
      #     url: http://external-service/v3/api-docs
      api-docs-path: "/v3/api-docs"        # 下游 api-docs 路径
    # 离线文档导出
    export:
      enabled: true                        # 是否启用导出端点
      base-path: "/doc/export"             # 导出路径前缀
      formats:                             # 允许的格式
        - html
        - markdown
        - json
```

### Reactive 配置（`aryee.doc.reactive.*`）

与 Blocking 一致，仅前缀由 `aryee.doc` 变为 `aryee.doc.reactive`。

## 访问地址

启动应用后，控制台会打印以下访问地址：

| 文档类型 | 默认路径 | 说明 |
|---------|---------|------|
| Swagger UI | `/swagger-ui.html` | SpringDoc 原生 UI |
| API Docs | `/v3/api-docs` | OpenAPI 3 JSON |
| Knife4j UI | `/doc.html` | Knife4j 增强 UI（推荐） |

## 使用示例

### 1. 标准接口（自动鉴权）

引入 Starter 后，所有 Controller 接口默认要求 Bearer Token：

```java
@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理", description = "用户 CRUD 接口")
public class UserController {

    @GetMapping("/{id}")
    @Operation(summary = "查询用户详情", description = "根据 ID 查询用户")
    @Parameter(name = "id", description = "用户 ID", required = true)
    public R<UserVO> getUser(@PathVariable Long id) {
        return R.ok(userService.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建用户")
    public R<Long> createUser(@Valid @RequestBody UserCreateDTO dto) {
        return R.ok(userService.create(dto));
    }
}
```

### 2. 隐藏接口

使用 `@DocHidden` 注解从文档中隐藏内部接口：

```java
@RestController
public class InternalController {

    @DocHidden  // 整个 Controller 从文档隐藏
    @GetMapping("/internal/debug")
    public R<String> debug() {
        return R.ok("debug info");
    }
}

@RestController
public class MixedController {

    @GetMapping("/public/info")
    public R<String> publicInfo() { ... }  // 文档可见

    @DocHidden  // 仅该方法隐藏
    @GetMapping("/internal/health")
    public R<String> health() { ... }
}
```

### 3. 角色可见性控制

使用 `@DocVisible` 配合 `aryee.doc.visibility.role` 配置控制接口可见性：

```java
// 仅管理员可见
@DocVisible(role = "admin")
@RestController
@RequestMapping("/admin")
public class AdminController { ... }

// 所有角色可见（默认）
@DocVisible
@GetMapping("/public/info")
public R<String> publicInfo() { ... }
```

配置 `aryee.doc.visibility.role=admin` 后，只有 `@DocVisible(role = "admin")` 或 `@DocVisible(role = "all")` 的接口会显示在文档中。

### 4. 敏感数据脱敏

使用 `@DocSensitive` 注解在文档中自动脱敏敏感字段：

```java
public class UserVO {
    @DocSensitive(mask = DocSensitive.MaskType.PHONE)
    private String phone;    // 文档显示: 138****1234

    @DocSensitive(mask = DocSensitive.MaskType.EMAIL)
    private String email;    // 文档显示: a***@example.com

    @DocSensitive(mask = DocSensitive.MaskType.ID_CARD)
    private String idCard;   // 文档显示: 110***********1234
}
```

### 5. 排除鉴权（公开接口）

```java
@PostMapping("/login")
@Operation(summary = "用户登录", description = "无需 Token 的公开接口")
@SecurityRequirements  // 空注解表示覆盖全局 Bearer 鉴权
public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
    return R.ok(authService.login(dto));
}
```

### 6. 网关文档聚合（微服务架构）

在网关应用中启用聚合，自动发现并展示所有下游服务的 API 文档：

```yaml
aryee:
  doc:
    gateway:
      enabled: true                        # 启用网关聚合
      exclude-services:                    # 排除不需要展示的服务
        - admin-service
      services:                            # 手动追加外部服务
        - name: external-api
          url: http://external-api.com/v3/api-docs
```

启用后，Foundation 自动提供以下端点：

| 端点 | 说明 |
|------|------|
| `/swagger-resources` | 兼容旧版 Knife4j 的文档资源列表 |
| `/v3/api-docs/swagger-config` | Knife4j 4.x 文档聚合配置（springdoc 协议） |

所有端点均通过 `DiscoveryClient` 过滤，仅返回有活跃注册实例的下游服务，避免 UI 尝试加载不可用服务的文档时报错。

> **重要约束**：自动发现的文档 URL 格式为 `/{routeId}/v3/api-docs`，route ID 必须与路由的 Path 谓词前缀一致，否则 Knife4j UI 请求文档时会 404。如果无法对齐，可使用 `gateway.services` 手动配置文档 URL。

## 自定义扩展

### 覆盖默认 OpenAPI Bean

```java
@Bean
public OpenAPI customOpenAPI() {
    return new OpenAPI()
            .info(new Info().title("自定义标题").version("2.0.0"));
    // 用户自定义 Bean 会覆盖 AryeeDocAutoConfiguration 的默认 Bean
}
```

### 新增分组

```java
@Bean
public GroupedOpenApi userGroup() {
    return GroupedOpenApi.builder()
            .group("user")
            .packagesToScan("cn.aryee.user.api")
            .build();
}
```

## 依赖要求

- Spring Boot 4.x
- Java 21+
- SpringDoc OpenAPI 2.8.6+（由 bom-base 管理）
- Knife4j 4.5.0+（由 bom-base 管理）
- Spring Cloud Gateway（可选，网关聚合功能需要）

## 与 BOM 集成

业务项目只需 import 对应 BOM 即可统一版本：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>cn.aryee.foundation</groupId>
            <artifactId>bom-apidoc</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

`bom-apidoc` 已自动 import `bom-base`，业务项目无需再 import `bom-base`。
