# Aryee WebSocket 实时通信模块

> **所属项目**: [Aryee Foundation](../../README.md)
> **架构层次**: 基础设施层 (Foundation Layer)
> **技术栈**: Java 21, Spring Boot 4.0.6, Spring WebSocket, STOMP, Reactor Netty
> **访问模式**: 阻塞式 (Servlet / Tomcat) / 响应式 (WebFlux / Reactor Netty)

## 简介

WebSocket 实时通信模块基于三层架构（API / Infrastructure / Autoconfigure）实现，提供统一的实时双向通信能力。Blocking 模式基于 Spring WebSocket + STOMP 运行于 Servlet 容器（Tomcat），Reactive 模式基于 Spring WebFlux + Reactor Netty 运行于响应式容器。

### 核心特性

- ✅ **三层架构**: API 契约层 + Infrastructure 实现层 + Autoconfigure 自动配置层
- ✅ **双模式隔离**: Blocking（Servlet/Tomcat）与 Reactive（WebFlux/Netty）接口与实现完全隔离，使用独立 Starter
- ✅ **统一服务接口**: `WebSocketService` / `ReactiveWebSocketService` 屏蔽底层容器差异
- ✅ **会话管理**: `WebSocketSessionManager` / `ReactiveWebSocketSessionManager` 提供在线用户管理、会话查询、踢出等功能
- ✅ **消息推送**: 支持点对点（sendToUser）、会话定向（sendToSession）、广播（broadcast）、主题订阅（sendToTopic）
- ✅ **STOMP 协议**: Blocking 模式原生支持 STOMP 协议，支持 `/topic`、`/queue`、`/app` 目的地前缀
- ✅ **心跳检测**: 可配置的心跳间隔与超时时间，自动维护连接活跃状态
- ✅ **握手鉴权**: `WebSocketHandshakeAuthenticator` SPI 与 security 模块联动，握手阶段鉴权
- ✅ **集群消息广播**: 基于 messaging 模块发布订阅的跨节点消息中继，零改造自动广播到集群所有节点
- ✅ **Spring Boot 自动配置**: 通过 `AutoConfiguration.imports` 自动装配，零配置开箱即用

## 模块结构

```
aryee-foundation-websocket/
├── websocket-api/                               # API 契约层
│   └── cn.aryee.websocket.api
│       ├── config/WebSocketProperties.java      # 配置前缀 aryee.websocket
│       ├── model/                               # 数据模型
│       │   ├── WebSocketMessage.java            # 消息模型（不可变值对象）
│       │   ├── WebSocketClusterMessage.java     # 集群中继消息模型
│       │   ├── OnlineUser.java                  # 在线用户
│       │   ├── WebSocketSessionInfo.java        # 会话信息
│       │   └── MessageType.java                 # 消息类型枚举
│       ├── service/                             # 服务契约
│       │   ├── WebSocketService.java            # Blocking 主接口
│       │   ├── ReactiveWebSocketService.java    # Reactive 主接口
│       │   ├── WebSocketSessionManager.java     # Blocking 会话管理
│       │   ├── ReactiveWebSocketSessionManager.java  # Reactive 会话管理
│       │   └── WebSocketHandshakeAuthenticator.java  # 握手鉴权 SPI
│       └── exception/WebSocketException.java
│
├── websocket-infrastructure/                    # 实现层
│   └── cn.aryee.websocket.infrastructure
│       ├── cluster/                             # 集群广播公共组件
│       │   └── WebSocketClusterMessageConverter.java  # 中继消息转换器
│       ├── blocking/tomcat/                     # Servlet 容器实现
│       │   ├── TomcatWebSocketConfig.java       # @EnableWebSocketMessageBroker
│       │   ├── TomcatWebSocketHandler.java      # 连接生命周期管理
│       │   ├── TomcatWebSocketService.java      # WebSocketService 实现
│       │   ├── TomcatWebSocketSessionManager.java  # 会话管理实现
│       │   └── TomcatWebSocketHandshakeInterceptor.java  # 握手拦截器
│       ├── blocking/cluster/                    # Blocking 集群中继
│       │   ├── WebSocketClusterRelay.java       # 集群消息中继器
│       │   └── ClusterAwareWebSocketService.java    # 集群感知装饰器
│       ├── reactive/netty/                      # Reactor Netty 实现
│       │   ├── NettyWebSocketConfig.java        # 端点映射
│       │   ├── NettyWebSocketHandler.java       # WebSocketHandler 实现
│       │   ├── NettyWebSocketService.java       # ReactiveWebSocketService 实现
│       │   └── NettyWebSocketSessionManager.java  # Reactive 会话管理实现
│       └── reactive/cluster/                    # Reactive 集群中继
│           ├── ReactiveWebSocketClusterRelay.java    # 响应式集群消息中继器
│           └── ClusterAwareReactiveWebSocketService.java  # 响应式集群感知装饰器
│
├── websocket-spring-boot-autoconfigure/         # Blocking 自动配置
│   └── cn.aryee.websocket.autoconfigure
│       └── AryeeWebSocketAutoConfiguration.java
├── websocket-reactive-spring-boot-autoconfigure/ # Reactive 自动配置
│   └── cn.aryee.websocket.reactive.autoconfigure
│       └── AryeeWebSocketReactiveAutoConfiguration.java
├── websocket-spring-boot-starter/               # Blocking Starter
└── websocket-reactive-spring-boot-starter/      # Reactive Starter
```

**自动配置注册**：
- Blocking: `cn.aryee.websocket.autoconfigure.AryeeWebSocketAutoConfiguration`
- Reactive: `cn.aryee.websocket.reactive.autoconfigure.AryeeWebSocketReactiveAutoConfiguration`

## 使用方法

### 引入依赖

#### Blocking（Servlet / Tomcat）

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>websocket-spring-boot-starter</artifactId>
</dependency>
```

#### Reactive（WebFlux / Reactor Netty）

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>websocket-reactive-spring-boot-starter</artifactId>
</dependency>
```

> ⚠️ Blocking 与 Reactive Starter 不可同时引入，按运行时容器二选一。

### 配置项

```yaml
aryee:
  websocket:
    enabled: true                    # 是否启用，默认 true
    path-prefix: /ws                 # 端点路径前缀
    endpoint: /ws/{business}         # 端点路径模板
    stomp-enabled: true              # 是否启用 STOMP（仅 Blocking 模式生效）
    stomp-destination-prefix: /topic,/queue    # STOMP 目的地前缀
    application-destination-prefix: /app       # STOMP 应用前缀
    heartbeat:
      interval-ms: 25000             # 心跳间隔（毫秒）
      timeout-ms: 60000              # 心跳超时（毫秒）
    auth:
      enabled: false                 # 是否启用握手鉴权
      header-name: Authorization     # 鉴权 Header 名
      query-param: token             # 鉴权 query 参数名
    cluster:
      enabled: false                 # 是否启用集群消息广播，默认 false
      topic: aryee-websocket-cluster # 集群中继消息主题
      instance-id: <随机UUID>        # 当前节点实例ID（防重复投递，默认自动生成）
```

## 代码示例

### Blocking 模式

#### 1. 实现握手鉴权 SPI

```java
@Component
public class JwtWebSocketAuthenticator implements WebSocketHandshakeAuthenticator {

    @Autowired
    private JwtTokenService jwtTokenService;

    @Override
    public String authenticate(Map<String, Object> handshakeAttributes) {
        String token = (String) handshakeAttributes.get("token");
        if (token == null || token.isEmpty()) {
            throw new WebSocketException("WebSocket 握手缺少 token");
        }
        return jwtTokenService.parseUserId(token);  // 返回 userId
    }
}
```

#### 2. 发送消息

```java
@Service
public class NotificationService {

    @Autowired
    private WebSocketService webSocketService;

    public void notifyUser(String userId, String message) {
        // 点对点发送给指定用户
        webSocketService.sendToUser(userId, message);
    }

    public void broadcast(String message) {
        // 广播给所有在线用户
        webSocketService.broadcast(message);
    }

    public void publishToTopic(String topic, Object payload) {
        // 发送到 STOMP 主题
        webSocketService.sendToTopic("/topic/orders", payload);
    }
}
```

#### 3. 查询在线用户

```java
@Service
public class OnlineUserService {

    @Autowired
    private WebSocketSessionManager sessionManager;

    public int getOnlineCount() {
        return sessionManager.getOnlineCount();
    }

    public List<OnlineUser> getOnlineUsersByTenant(String tenantId) {
        return sessionManager.getOnlineUsersByTenant(tenantId);
    }

    public boolean kickOut(String sessionId, String reason) {
        return sessionManager.kickOut(sessionId, "管理员踢出：" + reason);
    }
}
```

#### 4. 前端客户端（STOMP）

```javascript
// 使用 stompjs + sockjs-client
const socket = new SockJS('http://localhost:8080/ws/notification');
const stompClient = Stomp.over(socket);

stompClient.connect({ Authorization: 'Bearer ' + token },
    function(frame) {
        // 订阅主题
        stompClient.subscribe('/topic/orders', function(message) {
            console.log('收到订单消息:', JSON.parse(message.body));
        });

        // 发送消息
        stompClient.send('/app/chat', {}, JSON.stringify({
            content: 'Hello'
        }));
    }
);
```

### Reactive 模式

#### 1. 发送消息（响应式）

```java
@Service
public class ReactiveNotificationService {

    @Autowired
    private ReactiveWebSocketService webSocketService;

    public Mono<Void> notifyUser(String userId, String message) {
        return webSocketService.sendToUser(userId, message);
    }

    public Mono<Void> broadcast(String message) {
        return webSocketService.broadcast(message);
    }
}
```

#### 2. 查询在线用户（响应式）

```java
@Service
public class ReactiveOnlineUserService {

    @Autowired
    private ReactiveWebSocketSessionManager sessionManager;

    public Mono<Integer> getOnlineCount() {
        return sessionManager.getOnlineCount();
    }

    public Flux<OnlineUser> getOnlineUsersByTenant(String tenantId) {
        return sessionManager.getOnlineUsersByTenant(tenantId);
    }
}
```

## 集群消息广播

默认情况下，WebSocket 消息仅在当前节点本地投递。当应用以多副本集群部署时，
用户的连接分布在不同节点，单节点发送的消息无法触达其他节点上的在线用户。
开启集群广播后，消息发送动作会通过 [messaging 模块](messaging.md)
的发布订阅通道中继到集群内所有节点，实现跨节点广播。

### 工作原理

```
业务调用 webSocketService.broadcast(payload)
              ↓
ClusterAwareWebSocketService（装饰器）
    ├─→ 本地投递：delegate.broadcast(payload)        → 当前节点在线用户
    └─→ 集群中继：WebSocketClusterRelay.publish(...) → MessagePublisher
                        ↓ 发布到集群主题（如 Redis pub/sub）
              集群内其他节点的 WebSocketClusterRelay 订阅收到
                        ↓ 过滤本节点自己发布的消息（sourceInstanceId）
              其他节点本地投递：localService.broadcast(payload)
```

**关键特性**：
- **零改造接入**：通过 `BeanPostProcessor` 自动包装 `WebSocketService`，业务代码无需任何修改
- **防重复投递**：中继消息携带 `sourceInstanceId`，接收端过滤本节点自己发布的消息（本地已投递）
- **发送类方法参与中继**：`sendToUser` / `sendToSession` / `broadcast` / `sendToTopic` / `sendToUsers`
- **中继失败不影响主流程**：集群发布异常仅记录日志，本地消息发送结果不受影响
- **优雅降级**：未引入 messaging 发布器时记录告警并跳过集群装配，应用正常启动

### 前置条件

集群广播依赖 messaging 模块提供的发布订阅能力，需引入对应 starter 并配置发布器（如 Redis）：

```xml
<!-- Blocking 模式 -->
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>messaging-spring-boot-starter</artifactId>
</dependency>

<!-- Reactive 模式 -->
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>messaging-reactive-spring-boot-starter</artifactId>
</dependency>
```

### 启用配置

```yaml
aryee:
  websocket:
    cluster:
      enabled: true                  # 开启集群广播
      topic: aryee-websocket-cluster # 集群中继主题（集群内各节点需一致）
```

> 💡 `instance-id` 无需手动配置，默认随机生成；多副本部署时各节点实例ID自动不同，
> 从而正确过滤自己发布的消息。若通过环境变量注入固定实例ID，需确保各副本唯一。

## 安全管控

WebSocket 模块支持握手鉴权，与 [security 模块](security.md) 联动。

### 工作原理

```
客户端 → WebSocket 握手请求（携带 token）
              ↓
     TomcatWebSocketHandshakeInterceptor / NettyWebSocketHandler
              ↓
     WebSocketHandshakeAuthenticator.authenticate(attributes)
              ↓
     握手成功 → 注入 userId/tenantId 到会话属性
     握手失败 → 拒绝连接（401）
```

### 条件装配

| Bean | 条件 | 说明 |
|------|------|------|
| `AryeeWebSocketAutoConfiguration` | `aryee.websocket.enabled=true` + SERVLET Web 应用 | Blocking 自动配置 |
| `AryeeWebSocketReactiveAutoConfiguration` | `aryee.websocket.enabled=true` + REACTIVE Web 应用 | Reactive 自动配置 |
| `WebSocketHandshakeAuthenticator` | 容器中不存在该 Bean | 提供默认实现（返回 null，即不鉴权） |
| `TomcatWebSocketConfig` | `spring-websocket` 在类路径 | 注册 STOMP 端点 |
| `NettyWebSocketConfig` | `spring-webflux` 在类路径 | 注册 WebSocketHandlerMapping |
| 集群中继装饰器 | `aryee.websocket.cluster.enabled=true` + `MessagePublisher` 在类路径 | 包装 WebSocketService 实现跨节点广播 |

### 安全风险等级

| 操作 | 风险等级 | 建议措施 |
|------|---------|---------|
| 握手鉴权缺失 | 🔴 高 | 生产环境必须实现 `WebSocketHandshakeAuthenticator` |
| 跨域 `setAllowedOriginPatterns("*")` | 🟡 中 | 生产环境应限制为具体域名 |
| 未授权广播 | 🟡 中 | 业务层应在调用 `broadcast` 前校验权限 |

## 兼容性

| 容器 | Blocking (Servlet) | Reactive (WebFlux) |
|------|--------------------|--------------------|
| Tomcat 10+ | ✅ | ✅ |
| Jetty 11+ | ✅ | ✅ |
| Undertow | ✅ | ✅ |
| Reactor Netty | ❌ | ✅ |

| 环境 | 版本要求 |
|------|----------|
| JDK | 21+ |
| Spring Boot | 4.0.6 |
| Spring WebSocket | 6.x |
| Reactor Netty | 1.2+ |

### Blocking vs Reactive 选型

| 场景 | 推荐模式 | 说明 |
|------|---------|------|
| 传统 Spring MVC + Tomcat | Blocking | 原生 STOMP 协议，生态成熟 |
| Spring WebFlux + Netty | Reactive | 高并发、低资源占用 |
| 需要复杂消息路由（STOMP） | Blocking | Reactive 模式不内置 STOMP 支持 |
| 微服务网关透传 | Reactive | 与 gateway 模块一致使用响应式栈 |

## 错误码范围

WebSocket 模块错误码范围：`20000 ~ 20999`（由 `ServiceErrorCodeRangeEnum.WEBSOCKET` 分配）。
