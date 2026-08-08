# Aryee Foundation 跨架构迁移手册

> **版本**: 1.2.0+
> **更新日期**: 2026-08-05
> **所属项目**: [Aryee Foundation](../README.md)

## 概述

本手册详细说明如何在 Aryee Foundation 三种架构形态之间进行迁移。
Aryee Foundation 的设计目标是**业务代码零变更**，迁移的核心操作是**更换 Starter 依赖 + 调整配置**。

---

## 迁移总则

### 核心原则

1. **业务代码零变更**: Controller / Service / Repository 层代码无需修改
2. **BOM + 示例即架构**: 参考 `examples/architecture-*-example` 调整模块 Starter 组合
3. **配置分离**: 架构相关的配置（缓存类型、事务类型、注册中心）在 `application.yml` 中集中管理
4. **渐进式迁移**: 支持从单体 → 云原生 → 微服务逐步演进

### 兼容性保证

| 维度 | 保证 |
|------|------|
| API 兼容 | 所有 API 签名在跨架构迁移中不变 |
| SPI 兼容 | `ConfigurationSourceProvider` / `ServiceInstanceResolver` 等 SPI 接口不变 |
| 注解兼容 | `@AryeeTransactional` / `@Cacheable` 等注解用法不变 |
| 配置兼容 | `aryee.*` 配置前缀在所有架构中一致 |

---

## 迁移路径一：单体 → 云原生

### 迁移步骤

#### Step 1: 调整模块组合

参考目标架构的示例项目（`examples/architecture-*-example`），调整 pom.xml 中的模块 Starter 组合。例如从单体迁移到云原生：

```xml
<!-- 增加网关、监控模块 -->
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>gateway-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>monitoring-spring-boot-starter</artifactId>
</dependency>
```

#### Step 2: 调整配置

```yaml
# Before: 单体架构配置
aryee:
  cache:
    type: caffeine
  transaction:
    type: local
  transport:
    discovery:
      type: static

# After: 云原生架构配置
aryee:
  config:
    enabled: true
    kubernetes:
      enabled: true
      config-map-name: aryee-foundation-config
      refresh-interval-seconds: 30
  cache:
    type: caffeine            # 不变，Caffeine 仍适用
  transaction:
    type: local               # 不变，本地事务仍适用
  transport:
    discovery:
      type: static            # 不变，K8s 通过 Service 发现
```

#### Step 3: 创建 K8s ConfigMap

```yaml
# k8s/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: aryee-foundation-config
  namespace: default
data:
  application.yml: |
    aryee:
      cache:
        type: caffeine
        default-expiration: 3600
      transaction:
        type: local
```

#### Step 4: 部署到 K8s

```bash
# 构建 Docker 镜像
docker build -t my-app:latest .

# 部署 ConfigMap
kubectl apply -f k8s/configmap.yaml

# 部署应用
kubectl apply -f k8s/deployment.yaml
```

### 变更清单

| 变更项 | 单体 | 云原生 |
|--------|------|--------|
| Starter | `monolith-starter` | `cloudnative-starter` |
| `pom.xml` | 改一行依赖 | 同上 |
| `application.yml` | 本地配置 | 增加 ConfigMap 配置 |
| 新增文件 | 无 | `k8s/configmap.yaml` |
| 外部依赖 | 零 | K8s 集群 |
| 业务代码 | 不变 | 不变 |

### 回滚方案

```bash
# 回滚到单体：只需改回 Starter
# 同时删除 ConfigMap 相关配置
kubectl delete configmap aryee-foundation-config
```

---

## 迁移路径二：云原生 → 微服务

### 迁移步骤

#### Step 1: 调整模块组合

参考目标架构的示例项目（`examples/architecture-*-example`），调整 pom.xml 中的模块 Starter 组合。例如从云原生迁移到微服务：

```xml
<!-- 增加缓存、数据库、事务等模块 -->
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>cache-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>transaction-spring-boot-starter</artifactId>
</dependency>
```

#### Step 2: 调整配置

```yaml
# Before: 云原生架构配置
aryee:
  config:
    kubernetes:
      enabled: true
      config-map-name: aryee-foundation-config
  cache:
    type: caffeine
  transaction:
    type: local
  transport:
    discovery:
      type: static
  scheduler:
    type: quartz

# After: 微服务架构配置
aryee:
  config:
    enabled: true
    # 从 K8s ConfigMap 切换到 Nacos Config
    # 需要在 Nacos 上创建对应配置
  cache:
    type: redis                # Caffeine → Redis
    key-prefix: ms:my-service
  transaction:
    type: seata                # 本地事务 → Seata 分布式事务
  transport:
    discovery:
      type: nacos              # Static → Nacos 服务发现
    nacos:
      server-addr: localhost:8848
  scheduler:
    type: xxl-job              # Quartz → XXL-Job 分布式调度
    xxl-job:
      admin-addresses: http://xxl-job-admin:8088
```

#### Step 3: 搭建外部中间件

```bash
# 1. 启动 Nacos
docker run -d --name nacos -p 8848:8848 nacos/nacos-server:latest

# 2. 启动 MySQL
docker run -d --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root mysql:8

# 3. 启动 Redis
docker run -d --name redis -p 6379:6379 redis:7

# 4. 启动 Seata Server
docker run -d --name seata -p 8091:8091 seataio/seata-server:latest

# 5. 启动 XXL-Job Admin（可选）
docker run -d --name xxl-job-admin -p 8088:8088 xuxueli/xxl-job-admin:latest
```

#### Step 4: 拆分服务（可选）

如果从单体直接迁移到微服务，需要将单体拆分为多个微服务：

```java
// 示例：订单服务独立为微服务
@SpringBootApplication
@EnableDiscoveryClient  // Nacos 服务注册
@EnableFeignClients     // 服务间调用
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

### 变更清单

| 变更项 | 云原生 | 微服务 |
|--------|--------|--------|
| Starter | `cloudnative-starter` | `microservice-starter` |
| 配置中心 | K8s ConfigMap | Nacos Config |
| 缓存 | Caffeine | Redis |
| 事务 | 本地事务 | Seata |
| 服务发现 | K8s Service | Nacos |
| 调度 | Quartz | XXL-Job |
| 外部依赖 | K8s 集群 | Nacos + MySQL + Redis + Seata |
| 业务代码 | 不变 | 不变 |

### 回滚方案

```bash
# 1. 改回云原生 Starter
# 2. 恢复 K8s ConfigMap 配置
# 3. 恢复 Caffeine 缓存配置
# 4. 关闭外部中间件
docker stop nacos mysql redis seata xxl-job-admin
```

---

## 迁移路径三：单体 → 微服务（直接迁移）

### 迁移步骤

#### Step 1: 调整模块组合

参考目标架构的示例项目（`examples/architecture-*-example`），调整 pom.xml 中的模块 Starter 组合。例如从单体迁移到微服务：

```xml
<!-- 增加监控、网关模块 -->
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>monitoring-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>gateway-spring-boot-starter</artifactId>
</dependency>
```

#### Step 2: 完整配置变更

```yaml
# Before: 单体架构配置
spring:
  datasource:
    url: jdbc:h2:mem:testdb
  jpa:
    hibernate:
      ddl-auto: create-drop
aryee:
  cache:
    type: caffeine
  transaction:
    type: local
  transport:
    discovery:
      type: static
  scheduler:
    type: quartz

# After: 微服务架构配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/my_service
    username: root
    password: root
  jpa:
    hibernate:
      ddl-auto: update
    database-platform: org.hibernate.dialect.MySQLDialect
  data:
    redis:
      host: localhost
      port: 6379
aryee:
  cache:
    type: redis
    key-prefix: ms:my-service
  transaction:
    type: seata
  transport:
    discovery:
      type: nacos
    nacos:
      server-addr: localhost:8848
  scheduler:
    type: xxl-job
    xxl-job:
      admin-addresses: http://localhost:8088/xxl-job-admin
```

#### Step 3: 配置 Nacos 配置中心

在 Nacos 控制台创建配置：
- Data ID: `my-service.properties`
- Group: `DEFAULT_GROUP`
- 配置格式: `Properties`

```properties
aryee.cache.type=redis
aryee.transaction.type=seata
aryee.transport.discovery.type=nacos
```

#### Step 4: 数据迁移

```sql
-- 从 H2 导出数据到 MySQL
-- 使用 H2 的 SCRIPT 命令导出
SCRIPT TO '/tmp/h2-dump.sql'

-- 在 MySQL 中执行建表和数据导入
SOURCE /tmp/h2-dump.sql;
```

### 变更清单

| 变更项 | 单体 | 微服务 |
|--------|------|--------|
| Starter | `monolith-starter` | `microservice-starter` |
| 数据库 | H2 嵌入式 | MySQL 外部 |
| 缓存 | Caffeine | Redis |
| 事务 | 本地事务 | Seata 分布式事务 |
| 服务发现 | 静态配置 | Nacos |
| 调度 | Quartz | XXL-Job |
| 配置管理 | application.yml | Nacos Config |
| 外部依赖 | 零 | Nacos + MySQL + Redis + Seata |
| 业务代码 | 不变 | 不变 |

---

## 常见问题（FAQ）

### Q1: 迁移后业务代码需要修改吗？

**不需要。** Aryee Foundation 的 SPI 抽象层确保了业务代码与架构形态解耦：
- `@AryeeTransactional` 在 Local 和 Seata 模式下行为一致
- `@Cacheable` 在 Caffeine 和 Redis 模式下行为一致
- `ConfigurationSourceProvider` 在 Environment 和 K8s ConfigMap 下 API 一致

### Q2: 迁移过程中如何验证？

建议按以下步骤验证：
1. 在本地环境搭建目标架构的中间件
2. 更换 Starter 并调整配置
3. 运行现有单元测试和集成测试
4. 手动验证关键业务流程
5. 灰度发布到生产环境

### Q3: 迁移后性能会变化吗？

| 维度 | 预期变化 |
|------|---------|
| 单体 → 云原生 | 启动时间略增（Docker/K8s 开销），运行时性能相近 |
| 云原生 → 微服务 | 网络延迟增加（远程调用替代本地调用），但扩展性大幅提升 |
| 单体 → 微服务 | 同上，建议先做性能基准测试 |

### Q4: 可以混合使用架构形态吗？

**可以。** 不同微服务可以使用不同的 Starter：
- 订单服务（微服务）: `microservice-starter`
- 后台管理（单体）: `monolith-starter`
- 数据处理（云原生）: `cloudnative-starter`

### Q5: 迁移后如何回滚？

Aryee Foundation 支持**零停机回滚**：
1. 保留旧版本的 JAR 包
2. 恢复旧配置
3. 通过负载均衡切换流量
4. 验证新版本无问题后再删除旧版本

---

## 附录：迁移检查清单

### 单体 → 云原生

- [ ] 更换 Starter 为 `cloudnative-starter`
- [ ] 创建 K8s ConfigMap 配置
- [ ] 调整 `application.yml` 移除本地配置
- [ ] 构建 Docker 镜像
- [ ] 编写 K8s Deployment YAML
- [ ] 配置存活探针和就绪探针
- [ ] 配置 ConfigMap 热刷新
- [ ] 验证业务功能
- [ ] 灰度发布

### 云原生 → 微服务

- [ ] 更换 Starter 为 `microservice-starter`
- [ ] 搭建 Nacos 服务
- [ ] 搭建 MySQL 数据库
- [ ] 搭建 Redis 缓存
- [ ] 搭建 Seata Server
- [ ] 搭建 XXL-Job Admin（可选）
- [ ] 在 Nacos 上创建配置
- [ ] 调整缓存类型为 Redis
- [ ] 调整事务类型为 Seata
- [ ] 调整服务发现为 Nacos
- [ ] 调整调度为 XXL-Job
- [ ] 运行集成测试
- [ ] 性能基准测试
- [ ] 灰度发布

### 单体 → 微服务（直接迁移）

- [ ] 更换 Starter 为 `microservice-starter`
- [ ] 搭建所有外部中间件（Nacos + MySQL + Redis + Seata + XXL-Job）
- [ ] 数据迁移（H2 → MySQL）
- [ ] 配置迁移（application.yml → Nacos Config）
- [ ] 调整所有配置项
- [ ] 运行完整回归测试
- [ ] 性能基准测试
- [ ] 灰度发布

---

## 参考

- [架构选型指南](./guides/ARCHITECTURE_SELECTION_GUIDE.md) — 如何选择架构形态
- [单体架构示例../examples/architecture-monolith-example — monolith-starter 完整示例
- [云原生架构示例../examples/architecture-cloudnative-example — cloudnative-starter 完整示例
- [微服务架构示例../examples/architecture-microservice-example — microservice-starter 完整示例
- [架构演进计划](https://github.com/aryeecn/aryee-foundation) — 全架构演进路线图