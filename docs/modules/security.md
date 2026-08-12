# Aryee Security 安全基础设施模块

> **所属项目**: [Aryee Foundation](../../README.md)
> **技术栈**: Java 21、Spring Boot 4.0.7、JJWT 0.12.6、Sa-Token 1.45.0（适配器）、Keycloak 25.0.6（适配器）、OAuth2

## 简介

安全基础设施模块采用**精简核心 + 可插拔适配器**架构，提供统一的安全解决方案。核心模块基于 **JJWT + 内存/Redis** 构建本地安全能力，Sa-Token / Keycloak / OAuth 作为独立适配器按需引入。

```
用户引入 security-spring-boot-starter
  ├── 默认获得：本地 JWT 认证 + 权限管理 + Web 防护 + 签名加密（仅依赖 JJWT）
  ├── 可选引入 security-satoken → 切换为 Sa-Token 认证
  ├── 可选引入 security-keycloak → 切换为 Keycloak 认证
  └── 可选引入 security-oauth → 获得第三方 OAuth 登录能力
```

模块遵循 Aryee Foundation 三层架构规范，API 层只定义契约，Infrastructure 层提供核心实现，适配器模块各自独立装配，引入 jar 即生效。

### 核心特性

- **认证授权**：默认本地 JWT 认证，可选切换为 Sa-Token / Keycloak 适配器，零配置即可获得基础安全
- **JWT Token**：基于 JJWT 0.12.6 的 Token 生成 / 验证 / 刷新 / 黑名单机制
- **OAuth2.0**：独立 `security-oauth` 适配器，内置 GitHub / Google / 微信 / Gitee / 支付宝平台 Provider，支持 SPI 自定义扩展
- **接口幂等**：基于 `@Idempotent` 注解 + SpEL 表达式，支持 Token / 业务键 / 用户操作三类幂等键
- **多因素认证（MFA）**：TOTP / 短信 / 邮件 多因子认证，支持挑战-应答流程
- **加解密**：AES / RSA / MD5 / SHA-256 / BCrypt，支持 GCM 模式与密钥派生
- **签名验签**：HMAC-SHA256 签名，提供 Servlet 拦截器与 WebFlux 过滤器双实现
- **安全防护**：暴力破解防护、防重放、XSS 过滤、SQL 注入拦截
- **会话管理**：内存 / Redis 会话存储，支持 Token 黑名单与续期
- **审计日志**：安全事件审计、操作日志记录、敏感数据脱敏
- **安全上下文传播**：统一 `SecurityContextHolder` 支持 ThreadLocal + Reactor Context 双模式，自动传播 userId/tenantId，覆盖入站请求恢复、微服务间 Feign/RestTemplate/WebClient 出站拦截、异步线程上下文透传

## 模块结构

```
aryee-foundation-security/
├── security-api/                              # API 契约层（接口、注解、模型、配置属性）
├── security-infrastructure/                   # 核心实现（仅 JJWT + 内存/Redis，零外部框架依赖）
├── security-satoken/                          # [适配器] Sa-Token 认证
├── security-keycloak/                         # [适配器] Keycloak 认证
├── security-oauth/                            # [适配器] 第三方 OAuth 登录（GitHub/Google/微信/Gitee/支付宝）
├── security-spring-boot-autoconfigure/        # 阻塞式自动配置
├── security-reactive-spring-boot-autoconfigure/  # 响应式自动配置
├── security-spring-boot-starter/              # 阻塞式 Starter（Servlet / WebMVC）
└── security-reactive-spring-boot-starter/     # 响应式 Starter（WebFlux）
```

### 模块说明

| 模块 | artifactId | 说明 |
|------|------------|------|
| API | `security-api` | 对外契约：服务接口、注解、枚举、异常、配置属性、模型 |
| Infrastructure | `security-infrastructure` | 核心实现：Blocking（JWT / BCrypt）+ Reactive（JWT / BCrypt）+ 幂等 + 签名 + Web 防护 + 会话 + 审计 |
| Sa-Token 适配器 | `security-satoken` | Sa-Token 认证实现（Blocking + Reactive），`@ConditionalOnClass("cn.dev33.satoken.stp.StpUtil")` |
| Keycloak 适配器 | `security-keycloak` | Keycloak 认证实现（Blocking + Reactive），`@ConditionalOnClass("org.keycloak.admin.client.Keycloak")` |
| OAuth 适配器 | `security-oauth` | 第三方 OAuth 登录（GitHub/Google/微信/Gitee/支付宝），Blocking + Reactive 双模式 |
| Autoconfigure | `security-spring-boot-autoconfigure` | Servlet 自动配置，注册 `AryeeSecurityAutoConfiguration` / `AryeeIdempotentAutoConfiguration` |
| Reactive Autoconfigure | `security-reactive-spring-boot-autoconfigure` | WebFlux 自动配置 |
| Starter | `security-spring-boot-starter` | 阻塞式依赖聚合（核心 + 自动装配） |
| Reactive Starter | `security-reactive-spring-boot-starter` | 响应式依赖聚合 |

### security-api 包结构

```
cn.aryee.security.api/
├── annotation/          # @AnonymousAccess、@RequiresPermissions、@RequiresRoles、@RequiresAbacPermission、@SensitiveMeta
├── config/              # SecurityProperties、PasswordPolicy、SessionPolicy
├── constant/            # AuthConstants、JwtConstants、PermissionConstants、RoleConstants、TokenConstants
├── context/             # SecurityContextHolder、ReactiveSecurityUtils（安全上下文）
├── enums/               # AccountStatus、Logical、RequirePermissionMode、SensitiveType
├── exception/           # SecurityException
├── idempotent/          # 幂等模块：@Idempotent 注解、IdempotentService / IdempotentStore / ReactiveIdempotentService
├── model/               # AuthRequest/AuthResponse、UserPrincipal、SecurityAuditLog、MfaChallenge、AbacPolicy 等
├── oauth/               # OAuth2：OAuthAuthService、OAuthTokenStore、OAuthUserBindingService（含 Reactive 版本）
└── service/             # AuthService、TokenService、MfaService、CryptoService、SignatureService、PermissionManager 等
```

### security-infrastructure 包结构

```
cn.aryee.security.infrastructure/
├── blocking/                    # 阻塞式实现
│   ├── auth/                    # LocalAuthService（本地 JWT 认证，默认兜底）
│   ├── bcrypt/                  # BCryptCryptoService
│   ├── cache/                   # 基于 Cache 的安全服务（防暴力破解、会话、审计）
│   ├── context/                 # DefaultSecurityContextSnapshot（安全上下文 SPI 实现）
│   ├── crypto/                  # CryptoServiceImpl（AES / RSA / Base64）
│   ├── jwt/                     # JwtTokenService（JJWT 实现）
│   ├── permission/              # DefaultPermissionManager / InMemoryPermissionCacheService
│   ├── servlet/                 # Servlet 配置、XssFilter
│   ├── session/                 # InMemorySessionManagementService / RedisSessionManagementService
│   ├── signature/               # HmacSignatureService + SignatureInterceptor / SignatureAspect
│   ├── web/                     # CaptchaService、RateLimitInterceptor、WebSecurityAutoConfiguration
│   └── web/                     # 安全上下文传播
│       ├── filter/              # SecurityContextFilter（清理）、SecurityContextInboundFilter（入站恢复）
│       └── interceptor/         # SecurityContextFeignInterceptor、SecurityContextRestTemplateInterceptor（出站传播）
├── idempotent/                  # 幂等实现（Blocking + Reactive，内存 + Redis）
├── reactive/                    # 响应式实现
│   ├── auth/                    # ReactiveJwtAuthService
│   ├── bcrypt/                  # ReactiveBCryptCryptoService
│   ├── jwt/                     # ReactiveJwtTokenService
│   ├── signature/               # ReactiveSignatureService + SignatureWebFilter
│   └── webflux/                 # 响应式安全上下文传播
│       └── filter/              # SecurityContextWebFilter（清理）、ReactiveSecurityContextInboundFilter（入站恢复）、SecurityContextWebClientFilter（出站传播）
└── util/                        # HttpRequestSecurityUtil、XssFilterUtil
```

## 模块分层与引用规则

安全模块采用**契约层 + 核心实现 + 可插拔适配器**分层架构，不同架构场景下的引用方式不同。

### 模块分层

```
security-api                              ← 纯契约层（接口/注解/模型），所有消费方只依赖这一层
    ↑
security-infrastructure                   ← 核心实现（本地 JWT + JJWT + 内存/Redis）
    ↑
security-spring-boot-starter              ← 阻塞式 Starter（聚合 api + infrastructure + autoconfigure）
security-reactive-spring-boot-starter     ← 响应式 Starter（WebFlux 场景）

--- 以下为可选适配器，引入 jar + 配置即自动装配 ---
security-satoken                          ← Sa-Token 认证适配器（替换本地 JWT）
security-keycloak                         ← Keycloak 认证适配器（替换本地 JWT）
security-oauth                            ← 第三方 OAuth 登录适配器（GitHub/微信/Google/Gitee/支付宝）
```

### 跨模块依赖规则

- 功能模块引用 security 时，**只能依赖 `security-api`**（契约层）
- **禁止依赖** `security-infrastructure` 及任何适配器模块（`security-satoken` / `security-keycloak` / `security-oauth`）
- 实际安全实现由最终的应用工程引入 Starter + 适配器，通过 `@ConditionalOnBean` 条件装配

### OAuth 与认证适配器的关系

OAuth 适配器与 Sa-Token / Keycloak **不是互斥关系**，而是互补：

| 能力 | Sa-Token / Keycloak | OAuth |
|------|---------------------|-------|
| 职责 | **认证授权**（签发/验证 Token） | **第三方登录**（GitHub/微信等 OAuth 流程） |
| 接口 | `AuthService` / `ReactiveAuthService` | `OAuthAuthService` / `ReactiveOAuthAuthService` |
| 使用位置 | 网关 + 所有下游服务 | 通常在网关或统一认证服务 |
| 互斥关系 | Sa-Token 和 Keycloak **互斥**（同一时间只能用一个） | OAuth 与两者**不互斥**，可叠加 |

## Maven 依赖

### 场景一：单体架构（Monolith）

单体应用是一个完整的 Spring Boot 应用，认证/授权/OAuth 登录都在同一个进程内完成。

#### 1.1 单体 + 本地 JWT（零配置默认）

```xml
<!-- 仅需一个 Starter，即可获得完整安全能力 -->
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>security-spring-boot-starter</artifactId>
</dependency>
```

无需额外配置，默认使用本地 JWT 认证。适合内部系统、管理后台等不需要第三方登录的场景。

#### 1.2 单体 + Sa-Token

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>security-spring-boot-starter</artifactId>
</dependency>
<!-- Sa-Token 适配器：引入后切换认证实现 -->
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>security-satoken</artifactId>
</dependency>
<!-- 可选：第三方 OAuth 登录 -->
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>security-oauth</artifactId>
</dependency>
```

```yaml
aryee:
  security:
    auth:
      type: sa-token          # 切换认证实现为 Sa-Token
```

#### 1.3 单体 + Keycloak

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>security-spring-boot-starter</artifactId>
</dependency>
<!-- Keycloak 适配器：引入后切换认证实现 -->
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>security-keycloak</artifactId>
</dependency>
<!-- 可选：第三方 OAuth 登录（与 Keycloak SSO 互补） -->
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>security-oauth</artifactId>
</dependency>
```

```yaml
aryee:
  security:
    auth:
      type: keycloak          # 切换认证实现为 Keycloak
keycloak:
  auth-server-url: http://keycloak:8080/auth
  realm: my-realm
  resource: my-client-id
  credentials:
    secret: my-client-secret
```

#### 1.4 单体 + WebFlux（响应式）

将 `security-spring-boot-starter` 替换为 `security-reactive-spring-boot-starter`，适配器引用方式不变。每个适配器内部均有 Blocking + Reactive 双模式实现，自动按 classpath 装配。

### 场景二：微服务架构（Microservices）

微服务拆分为**网关**和**下游服务**，安全职责分离：

```
客户端请求 → Gateway（WebFlux）
              ├─ AuthGatewayFilter        ← ReactiveAuthService 鉴权
              ├─ SameTokenForwardFilter   ← ReactiveSameTokenService 注入 Same-Token
              └─ ApiSignatureFilter       ← ReactiveSignatureService 签名验证
                    │
                    ↓ Same-Token 头 + 安全上下文
              ┌─────┼─────┐
              ↓     ↓     ↓
           服务A  服务B  服务C  ← 各自验证 Token / 恢复安全上下文
```

#### 2.1 网关服务

网关**不需要**引入 `security-spring-boot-starter`。`gateway-reactive-spring-boot-starter` 已传递依赖 `security-api`（契约层），实际认证实现由适配器模块提供。

```xml
<!-- 网关 Starter（已聚合 gateway 核心 + security-api 契约） -->
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>gateway-reactive-spring-boot-starter</artifactId>
</dependency>

<!-- 认证适配器：按需选择一个 -->
<!-- 方案 A：Sa-Token（提供 ReactiveAuthService + ReactiveSameTokenService） -->
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>security-satoken</artifactId>
</dependency>

<!-- 方案 B：Keycloak（提供 ReactiveAuthService，无 SameTokenService） -->
<!--
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>security-keycloak</artifactId>
</dependency>
-->

<!-- 可选：OAuth 第三方登录（网关统一入口处理登录跳转） -->
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>security-oauth</artifactId>
</dependency>
```

> **Same-Token 说明**：`SameTokenForwardFilter` 通过 `ReactiveSameTokenService` 抽象获取 Same-Token，网关本身不依赖任何具体安全框架。当前仅 Sa-Token 适配器提供该实现，Keycloak 适配器未提供（网关使用 Keycloak 时 Same-Token 过滤器不会装配）。

#### 2.2 下游微服务（Servlet 栈）

下游服务需要完整的安全能力（Token 验证、权限检查、安全上下文恢复与传播）：

```xml
<!-- 下游服务需要完整 Starter -->
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>security-spring-boot-starter</artifactId>
</dependency>
<!-- 与网关保持一致的认证适配器（用于验证 Same-Token 等） -->
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>security-satoken</artifactId>
</dependency>
```

```yaml
aryee:
  security:
    inbound-enabled: true     # 启用入站上下文恢复（从网关传播的 header 恢复 userId/tenantId）
    outbound-enabled: true    # 启用出站上下文传播（Feign 调用其他服务时自动携带）
    auth:
      type: sa-token          # 与网关保持一致
```

#### 2.3 下游微服务（WebFlux 栈）

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>security-reactive-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>security-satoken</artifactId>
</dependency>
```

### 引用矩阵速查表

| 场景 | 基础 Starter | 认证适配器 | OAuth | 说明 |
|------|-------------|-----------|-------|------|
| 单体 + 本地 JWT | `security-spring-boot-starter` | 无需额外引入 | 按需 | 零配置即可使用 |
| 单体 + Sa-Token | `security-spring-boot-starter` | `security-satoken` | 按需 | `auth.type=sa-token` |
| 单体 + Keycloak | `security-spring-boot-starter` | `security-keycloak` | 按需 | `auth.type=keycloak` |
| 单体 + WebFlux | `security-reactive-spring-boot-starter` | 同上 | 按需 | 适配器内含 Reactive 双模式 |
| 微服务·网关 | `gateway-reactive-spring-boot-starter` | `security-satoken` 或 `security-keycloak` | 按需 | 不需要 security-starter |
| 微服务·下游（Servlet） | `security-spring-boot-starter` | 与网关一致 | 通常不需要 | Token 验证 + 上下文传播 |
| 微服务·下游（WebFlux） | `security-reactive-spring-boot-starter` | 与网关一致 | 通常不需要 | 同上 |

> 版本由 `bom-security` 统一锁定，无需声明 `<version>`。

## 配置项

配置前缀：`aryee.security`，对应 `SecurityProperties`。

```yaml
aryee:
  security:
    enabled: true                        # 是否启用安全模块

    # ========== 安全上下文传播配置 ==========
    inbound-enabled: true                # 是否启用入站安全上下文过滤器（从请求头恢复上下文）
    outbound-enabled: true               # 是否启用出站安全上下文拦截器（微服务间调用传播上下文）

    jwt:
      secret: ${JWT_SECRET:please-change-in-production}   # JWT 签名密钥（生产必须修改）
      access-token-expiration: 3600      # Access Token 过期时间（秒）
      refresh-token-expiration: 86400    # Refresh Token 过期时间（秒）
      blacklist-enabled: true            # 是否启用 Token 黑名单
      algorithm: HS512                   # 签名算法
      issuer: aryee-foundation           # JWT 签发者

    password-policy:                     # 密码策略（PasswordPolicy）
      # 见 PasswordPolicy.java

    session-policy:                      # 会话策略（SessionPolicy）
      # 见 SessionPolicy.java

    auth:
      type: local                        # 认证实现类型：local（默认） / sa-token / keycloak
      mfa-enabled: false                 # 是否启用多因素认证
      default-mfa-type: TOTP            # 默认 MFA 类型
      account-lock-enabled: true         # 是否启用账户锁定
      max-login-attempts: 5              # 登录失败最大次数
      lock-duration-minutes: 30          # 锁定时间（分钟）
      ip-whitelist-enabled: false        # 是否启用 IP 白名单
      ip-whitelist: []                   # IP 白名单
      device-fingerprint-enabled: false  # 是否启用设备指纹验证
      remember-me-days: 30               # 记住登录时间（天）
      login-log-enabled: true           # 是否启用登录日志

    # OAuth 登录配置（需引入 security-oauth 适配器）
    oauth:
      enabled: true                      # 是否启用 OAuth 登录（默认启用）

# Sa-Token 配置（需引入 security-satoken 适配器 + auth.type=sa-token）
# aryee:
#   security:
#     sa-token:
#       interceptor-enabled: true        # 是否启用 Sa-Token 拦截器

# Keycloak 配置（需引入 security-keycloak 适配器 + auth.type=keycloak）
# keycloak:
#   auth-server-url: http://localhost:8080/auth
#   realm: master
#   resource: admin-cli
#   credentials.secret: ""
```

## 核心 API

### AuthService（认证服务）

```java
public interface AuthService {
    AuthResponse login(AuthRequest request);
    void logout(String token);
    boolean verifyToken(String token);
    AuthResponse refreshToken(String refreshToken);
    UserPrincipal getUserInfo(String token);
    boolean hasPermission(String token, String permission);
    boolean hasRole(String token, String role);
}
```

Blocking 实现可选：`LocalAuthService`（默认，本地 JWT）、通过适配器引入 `SaTokenAuthService` / `KeycloakAuthService`，通过 `AuthServiceFactory` 工厂按优先级自动选择；Reactive 对应 `ReactiveAuthService`（默认 `ReactiveJwtAuthService`）。

### TokenService（JWT 令牌服务）

```java
public interface TokenService {
    String generateToken(String userId, Map<String, Object> payload);
    boolean validateToken(String token);
    Map<String, Object> parseToken(String token);
    String getUserIdFromToken(String token);
    String refreshToken(String token);
    void invalidateToken(String token);
}
```

默认实现：Blocking `JwtTokenService`、Reactive `ReactiveJwtTokenService`（均基于 JJWT 0.12.6）。

### MfaService（多因素认证）

```java
public interface MfaService {
    List<MfaType> getSupportedMfaTypes(String userId);
    MfaChallenge initiateChallenge(String userId, MfaType mfaType);
    boolean verifyChallenge(String challengeId, String verificationCode);
    Map<String, Object> bindMfa(String userId, MfaType mfaType, Map<String, Object> config);
    void unbindMfa(String userId, MfaType mfaType);
    Map<String, String> generateTotpSecret(String userId, String account, String issuer);
    boolean verifyTotpCode(String secretKey, String code);
    boolean sendSmsCode(String phoneNumber, String templateCode);
    boolean sendEmailCode(String email, String templateCode);
}
```

默认实现：`DefaultMfaService`（TOTP / SMS / EMAIL）。

### OAuthAuthService（OAuth2 认证）

```java
public interface OAuthAuthService {
    R<OAuthUser> oauthLogin(String platform, AuthCallback callback);
    String getAuthorizeUrl(String platform, String state);
}
```

内置平台 Provider：`GitHubOAuthPlatformProvider`、`GoogleOAuthPlatformProvider`、`WechatOAuthPlatformProvider`、`GiteeOAuthPlatformProvider`、`AlipayOAuthPlatformProvider`；可通过 SPI 实现 `OAuthServiceProvider` 自定义扩展。

> OAuth 实现位于独立适配器 `security-oauth`，需显式引入。

### CryptoService（加解密）

```java
public interface CryptoService {
    String md5(String data);
    String sha256(String data);
    String aesEncrypt(String data, String key);
    String aesDecrypt(String data, String key);
    String generateAesKey();
    String rsaEncrypt(String data, String publicKey);
    String rsaDecrypt(String data, String privateKey);
    String[] generateRsaKeyPair();
    String aesGcmEncrypt(String data, String key);   // GCM 模式
    String deriveKey(String password, String salt);  // 密钥派生
}
```

### SignatureService（签名验签）

```java
public interface SignatureService {
    SignatureInfo generateSignature(String accessKeyId, String accessKeySecret,
                                    String method, String url,
                                    Map<String, Object> params, String body);
    boolean verifySignature(SignatureInfo signatureInfo, String accessKeySecret,
                            String method, String url,
                            Map<String, Object> params, String body);
    String[] getSupportedMethods();
    String computeBodyHash(String body, String algorithm);
    String generateTimestamp();
    boolean validateTimestamp(String timestamp, long maxAgeSeconds);
}
```

默认实现：`HmacSignatureService`（HMAC-SHA256）；Blocking 通过 `SignatureInterceptor` 拦截，Reactive 通过 `SignatureWebFilter` 过滤。

### SecurityContextHolder（安全上下文持有者）

```java
// 获取当前用户ID
String userId = SecurityContextHolder.getUserId();
String userId = SecurityContextHolder.getUserIdOrDefault("system");  // 带默认值

// 获取当前租户ID
String tenantId = SecurityContextHolder.getTenantId();

// 设置上下文
SecurityContextHolder.setUserId("user-123");
SecurityContextHolder.setTenantId("tenant-456");

// 执行带上下文的操作
SecurityContextHolder.executeWith("user-123", () -> {
    // 业务逻辑，在此范围内可获取 userId
    String currentUserId = SecurityContextHolder.getUserId(); // "user-123"
});

// 快照机制（用于异步线程）
SecurityContextHolder.Snapshot snapshot = SecurityContextHolder.capture();
// ... 异步线程中 ...
SecurityContextHolder.restore(snapshot);
```

**核心特性**：
- 同时支持 ThreadLocal（Blocking）和 Reactor Context（Reactive）双模式
- 支持 userId 和 tenantId 双维度上下文
- 提供 Snapshot 快照机制，用于异步线程上下文透传
- 自动清理：通过 `SecurityContextFilter` / `SecurityContextWebFilter` 在请求结束后清理

### ReactiveSecurityUtils（响应式安全上下文工具）

```java
// 在 Reactor 流水线中写入上下文
Mono.just(data)
    .contextWrite(ReactiveSecurityUtils.withUserId("user-123"));

// 同时写入 userId 和 tenantId
Mono.just(data)
    .contextWrite(ReactiveSecurityUtils.withContext("user-123", "tenant-456"));

// 从 Reactor Context 读取
ReactiveSecurityUtils.getUserId()       // 返回 Mono<String>
ReactiveSecurityUtils.getTenantId()     // 返回 Mono<String>

// 对 Mono/Flux 包装上下文
Mono<User> mono = ReactiveSecurityUtils.withContext("user-123", userMono);
```

## 使用示例

### 1. 注解式权限控制

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/{id}")
    @RequiresPermissions("user:view")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions(value = {"user:delete", "admin"}, logical = Logical.OR)
    public void deleteUser(@PathVariable Long id) {
        userService.delete(id);
    }

    @GetMapping("/admin")
    @RequiresRoles("ADMIN")
    public List<User> listAll() {
        return userService.findAll();
    }

    @PostMapping("/public/health")
    @AnonymousAccess   // 跳过认证
    public String health() {
        return "ok";
    }
}
```

### 2. JWT 登录与刷新

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestParam String refreshToken) {
        return authService.refreshToken(refreshToken);
    }

    @PostMapping("/logout")
    public void logout(@RequestHeader("Authorization") String token) {
        authService.logout(token.replace("Bearer ", ""));
    }
}
```

### 3. 接口幂等

```java
@PostMapping("/api/orders")
@Idempotent(
    key = "#userId + ':' + #request.orderNo",   // SpEL 表达式
    expireTime = 30,
    timeUnit = TimeUnit.SECONDS,
    throwOnDuplicate = true,
    duplicateMessage = "订单正在处理中，请勿重复提交"
)
public Order createOrder(String userId, OrderRequest request) {
    return orderService.create(request);
}
```

幂等存储支持：`InMemoryIdempotentStore`（单机）与 `RedisIdempotentStore`（分布式），通过 `aryee.security.idempotent.*` 自动切换。

### 4. OAuth2 第三方登录

```java
@Autowired
private OAuthAuthService oauthAuthService;

@GetMapping("/oauth/authorize/{platform}")
public String authorize(@PathVariable String platform) {
    return oauthAuthService.getAuthorizeUrl(platform, state);
}

@GetMapping("/oauth/callback/{platform}")
public R<OAuthUser> callback(@PathVariable String platform, AuthCallback callback) {
    return oauthAuthService.oauthLogin(platform, callback);
}
```

### 5. MFA 绑定与验证

```java
@Autowired
private MfaService mfaService;

// 绑定 TOTP（生成二维码）
public Map<String, String> bindTotp(String userId, String account) {
    return mfaService.generateTotpSecret(userId, account, "aryee-app");
}

// 校验 TOTP 验证码
public boolean verify(String secretKey, String code) {
    return mfaService.verifyTotpCode(secretKey, code);
}
```

### 6. 安全上下文传播

#### 6.1 入站：从请求头恢复上下文

微服务接收网关请求时，`SecurityContextInboundFilter` 自动从请求头读取 `X-User-Id` 和 `X-Tenant-Id` 并写入 `SecurityContextHolder`：

```java
// 过滤器自动注册，无需额外配置
// 网关转发请求时已注入 X-User-Id / X-Tenant-Id 请求头
// SecurityContextInboundFilter 自动读取并写入 SecurityContextHolder

@RestController
public class UserController {
    
    @GetMapping("/profile")
    public UserProfile getProfile() {
        // 直接获取当前用户ID，无需手动解析请求头
        String userId = SecurityContextHolder.getUserId();
        String tenantId = SecurityContextHolder.getTenantId();
        return userService.getProfile(userId, tenantId);
    }
}
```

#### 6.2 出站：微服务间调用传播上下文

使用 Feign / RestTemplate / WebClient 调用下游服务时，出站拦截器自动注入安全上下文请求头：

```java
// Feign 调用（拦截器自动注册）
@FeignClient(name = "order-service")
public interface OrderClient {
    
    @GetMapping("/api/orders")
    List<Order> getOrders();
    // SecurityContextFeignInterceptor 自动注入 X-User-Id / X-Tenant-Id 请求头
}

// RestTemplate 调用
@Autowired
private RestTemplate restTemplate;

public List<Order> getOrders() {
    // SecurityContextRestTemplateInterceptor 自动注入请求头
    return restTemplate.getForObject("http://order-service/api/orders", List.class);
}

// WebClient 调用（响应式）
@Autowired
private WebClient webClient;

public Mono<List<Order>> getOrders() {
    // SecurityContextWebClientFilter 自动注入请求头
    return webClient.get()
        .uri("http://order-service/api/orders")
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<Order>>() {});
}
```

#### 6.3 异步线程上下文透传

使用 `@Async` 或 `CompletableFuture` 时，通过 `SpringAsyncUtil` 自动透传安全上下文：

```java
@Service
public class NotificationService {
    
    @Autowired
    private SpringAsyncUtil springAsyncUtil;
    
    public void sendNotificationAsync(String message) {
        // SpringAsyncUtil.wrap() 自动捕获并恢复 SecurityContext
        CompletableFuture.runAsync(SpringAsyncUtil.wrap(() -> {
            String userId = SecurityContextHolder.getUserId();  // 在异步线程中也能获取
            notificationSender.send(userId, message);
        }));
    }
}
```

#### 6.4 手动上下文控制

```java
// 临时切换用户上下文
SecurityContextHolder.executeWith("impersonated-user", () -> {
    // 在此范围内，所有代码获取的 userId 都是 "impersonated-user"
    auditService.logAction("user-impersonation", "impersonated-user");
});

// 多租户场景
SecurityContextHolder.executeWith("user-123", "tenant-456", () -> {
    // 同时设置 userId 和 tenantId
    dataService.query();  // 自动按租户隔离数据
});
```

## 自动装配

### 阻塞式自动装配

通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册：

- `cn.aryee.security.autoconfigure.AryeeSecurityAutoConfiguration`：核心安全配置，默认装配 MFA / 权限缓存 / 审计 / 防重放 / 防暴力破解 / 会话管理等内存实现
  - **入站过滤器**（`aryee.security.inbound-enabled=true`）：`SecurityContextInboundFilter`（从请求头恢复上下文）、`SecurityContextFilter`（清理 ThreadLocal）
  - **出站拦截器**（`aryee.security.outbound-enabled=true`）：`SecurityContextFeignInterceptor`（Feign）、`SecurityContextRestTemplateInterceptor`（RestTemplate）
- `cn.aryee.security.autoconfigure.idempotent.AryeeIdempotentAutoConfiguration`：幂等自动配置

### 适配器自动装配

每个适配器模块包含自己的 `AutoConfiguration`，引入 jar 即自动激活。所有适配器均支持 Blocking + Reactive 双模式，Reactive 配置通过嵌套静态类实现（条件：`@ConditionalOnClass("reactor.core.publisher.Mono")`）：

- `security-satoken`：`SaTokenAutoConfiguration`（条件：`@ConditionalOnClass("cn.dev33.satoken.stp.StpUtil")` + `auth.type=sa-token`）
  - 嵌套 `ReactiveSaTokenConfiguration`：注册 `ReactiveSaTokenAuthService` + `ReactiveSaTokenAuthServiceFactory`
- `security-keycloak`：`KeycloakAutoConfiguration`（条件：`@ConditionalOnClass("org.keycloak.admin.client.Keycloak")` + `auth.type=keycloak`）
  - 嵌套 `ReactiveKeycloakConfiguration`：注册 `ReactiveKeycloakAuthService` + `ReactiveKeycloakAuthServiceFactory`
- `security-oauth`：`OAuthAutoConfiguration`（条件：`@ConditionalOnClass("com.fasterxml.jackson.databind.ObjectMapper")` + `oauth.enabled=true`，默认启用）
  - 嵌套 `ReactiveOAuthConfiguration`：注册 `ReactiveOAuthAuthServiceImpl` + `ReactiveOAuthAuthServiceFactory`

### 响应式自动装配

- `cn.aryee.security.reactive.autoconfigure.AryeeSecurityReactiveAutoConfiguration`：响应式签名、XSS 过滤
- `cn.aryee.security.reactive.autoconfigure.idempotent.AryeeIdempotentReactiveAutoConfiguration`：响应式幂等
- **响应式安全上下文**：`ReactiveSecurityContextInboundFilter`（入站恢复）、`SecurityContextWebFilter`（清理）、`SecurityContextWebClientFilter`（出站传播）

所有 Bean 使用 `@ConditionalOnMissingBean` 装配，可通过自定义 `@Bean` 覆盖默认实现。

## 兼容性

| 技术组件 | 版本要求 |
|---------|---------|
| Java | 21+ |
| Spring Boot | 4.0.7 |
| Sa-Token | 1.45.0 |
| JJWT | 0.12.6 |
| Servlet 容器 | Tomcat 10.x / Jetty 11.x / Undertow 2.x |
| Reactive 运行时 | Netty / WebFlux |
