# 快速开始

本教程带你从零接入 Aryee Foundation。全程约 10 分钟。

## 1. 环境要求

| 项 | 要求 |
|----|------|
| JDK | 21+ |
| Maven | 3.8+ |
| IDE | IntelliJ IDEA（推荐） |
| Spring Boot | 4.0.x（框架基于 Spring Boot 4.0.6 构建） |

## 2. 配置快照仓库

当前框架发布的是 **SNAPSHOT 版本**（`1.0.0-SNAPSHOT`），托管在 Maven Central 快照仓。
Maven 默认只解析正式仓库，需要在消费方项目中显式启用快照仓。

**方式一（推荐）**：在消费方项目 `pom.xml` 中添加：

```xml
<repositories>
    <repository>
        <id>central-portal-snapshots</id>
        <name>Central Portal Snapshots</name>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <releases><enabled>false</enabled></releases>
        <snapshots><enabled>true</enabled></snapshots>
    </repository>
</repositories>
```

**方式二**：在 `~/.m2/settings.xml` 的 `<profiles>` 中配置并激活，对该机器上所有项目生效：

```xml
<profile>
    <id>central-snapshots</id>
    <repositories>
        <repository>
            <id>central-portal-snapshots</id>
            <url>https://central.sonatype.com/repository/maven-snapshots/</url>
            <releases><enabled>false</enabled></releases>
            <snapshots><enabled>true</enabled></snapshots>
        </repository>
    </repositories>
</profile>

<!-- 在 activeProfiles 中激活 -->
<activeProfiles>
    <activeProfile>central-snapshots</activeProfile>
</activeProfiles>
```

> 正式版（1.0.0）发布后将直接走 Maven Central 正式仓，无需任何额外仓库配置。

## 3. 引入依赖

### Step 1：引入 BOM 统一版本管理

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>cn.aryee.foundation</groupId>
            <artifactId>bom-full</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

> 只需按模块裁剪时，也可以 import 单模块 BOM（如 `bom-cache`、`bom-database`），
> 完整 BOM 体系见 [bom 模块文档](modules/bom.md)。

### Step 2：按需引入 Starter

版本由 BOM 统一管理，**无需写 `<version>`**：

```xml
<!-- 缓存模块（Blocking 模式） -->
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>cache-spring-boot-starter</artifactId>
</dependency>

<!-- 数据库模块 -->
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>database-spring-boot-starter</artifactId>
</dependency>
```

**Reactive（WebFlux）项目**请引入对应的 reactive starter，并**不要**同时引入 blocking starter：

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>cache-reactive-spring-boot-starter</artifactId>
</dependency>
```

## 4. 第一个功能：缓存

### 配置

`application.yml` 零配置即可启动（默认 memory 内存缓存）：

```yaml
aryee:
  cache:
    enabled: true
    type: memory        # memory / caffeine / redis / multi
```

切换 Redis 实现：

```yaml
aryee:
  cache:
    type: redis
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
```

### 使用

```java
@Service
public class UserService {

    private final CacheService cacheService;

    public UserService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    public User getUser(String userId) {
        User user = cacheService.get("user:" + userId, User.class);
        if (user == null) {
            user = userRepository.findById(userId);
            cacheService.put("user:" + userId, user);
        }
        return user;
    }
}
```

Reactive 模式注入 `ReactiveCacheService`，方法返回 `Mono<T>` / `Flux<T>`，接口一一对应。

更多缓存能力（多级缓存、声明式 `@Cacheable`、分布式锁、三大防护）见
[缓存模块文档](modules/cache.md)。

## 5. 运行官方示例

本仓库 [examples/](../examples) 目录提供 30+ 可直接运行的示例工程，每个都是独立 Maven 项目。

**推荐起点**——单体架构示例（H2 嵌入式数据库，零外部依赖）：

```bash
cd examples/architecture-monolith-example
mvn spring-boot:run
```

按模块学习的示例对照表见 [examples/README.md](../examples/README.md)。

> 示例工程引用的构件版本为 `1.0.0-SNAPSHOT`，请先完成第 2 节的快照仓配置。

## 6. 下一步

- 📚 [文档中心](README.md)：全部模块文档与架构指南
- 🏗️ [架构选型指南](guides/ARCHITECTURE_SELECTION_GUIDE.md)：单体 / 微服务 / 云原生怎么选
- 🔀 [跨架构迁移手册](guides/MIGRATION_GUIDE.md)：已有项目如何接入与切换架构形态
- ❓ 遇到问题请到 [GitHub Issues](https://github.com/aryeecn/aryee-foundation/issues) 反馈
