# Aryee Scheduler 任务调度基础设施模块

> **所属项目**: [Aryee Foundation](../../README.md)
> **技术栈**: Java 21、Spring Boot 4.0.7、Quartz、XXL-Job、Reactor

## 简介

任务调度基础设施模块提供统一的分布式任务调度解决方案，基于 **Quartz** 与 **XXL-Job** 双引擎构建，支持 Cron 调度、任务分片、DAG 依赖、集群故障转移、失败重试等能力。模块采用 Blocking / Reactive 双模式设计，通过统一的 `JobService` / `ReactiveJobService` 契约屏蔽底层调度器差异，业务代码可在两个引擎间无缝切换。

### 核心特性

- ✅ **双调度引擎**: Quartz（单体 / 简单分布式）与 XXL-Job（可视化分布式调度中心）可按 `type` 一键切换
- ✅ **多任务类型 SPI**: 通过 `JobHandler` SPI 支持 JAVA / HTTP / SCRIPT / BEAN_METHOD / DATA_PIPELINE 五类任务
- ✅ **统一调度契约**: `JobService`（Blocking）与 `ReactiveJobService`（Reactive）方法对齐，返回 `Mono` / `Flux`
- ✅ **批量与查询能力**: 批量暂停 / 恢复 / 删除 / 触发，按组 / 状态 / 标签 / 关键字过滤
- ✅ **重试与退避**: `RetryPolicy` 支持 FIXED / EXPONENTIAL / RANDOM 三种退避策略
- ✅ **集群与分片**: 集群节点检查、故障转移、负载均衡（ROUND_ROBIN / RANDOM / LEAST_LOADED / CONSISTENT_HASH）、任务分片
- ✅ **DAG 依赖链**: 任务间依赖编排（`dependencyEnabled`）
- ✅ **调度器管理**: `SchedulerManagementService` 提供生命周期、健康检查、监听器管理
- ✅ **执行监控**: 执行日志、统计（成功 / 失败 / 耗时）、失败率告警阈值
- ✅ **安全管控（可选）**: 委托 security 模块进行任务操作权限检查和审计日志（`aryee.scheduler.security.enabled=true`）
- ✅ **双模式隔离**: Blocking / Reactive 严格分层，独立 Starter，禁止同时引入

## 架构定位

- **依赖方向**: `Starter → Autoconfigure → Infrastructure → API`
- **公共基础**: 依赖 `commons-core`（`JobType` 实现 `EnumService<String>`）
- **版本管理**: 由 `aryee-foundation-bom` 统一管理，子模块依赖禁止声明 `<version>`
- **双模式隔离**: Blocking 与 Reactive 使用独立的 Starter / Autoconfigure 模块，用户二选一引入

## 模块结构

```
aryee-foundation-scheduler/                              # 聚合 POM (artifactId=aryee-foundation-scheduler)
├── scheduler-api/                                       # API 契约层
│   └── cn.aryee.scheduler.api/
│       ├── config/SchedulerProperties.java              # 配置属性 (prefix=aryee.scheduler)
│       ├── constant/ScheduleConstants.java              # 调度常量
│       ├── enums/JobType.java                           # 任务类型枚举 (JAVA/HTTP/SCRIPT/BEAN_METHOD/DATA_PIPELINE)
│       ├── exception/                                   # 调度异常
│       ├── model/                                       # 数据模型
│       │   ├── JobInfo / JobStatus / TaskInfo
│       │   ├── JobExecutionLog / JobExecutionStats
│       │   ├── JobExecutionContext
│       │   ├── RetryPolicy                              # 重试策略（含 BackoffType）
│       │   ├── BeanMethodJobConfig / HttpJobConfig / ScriptJobConfig  # 各任务类型配置
│       │   ├── JobDependency                            # DAG 依赖
│       │   ├── JobShardInfo                             # 分片信息
│       │   └── ClusterNodeInfo                          # 集群节点信息
│       ├── service/                                     # 服务契约
│       │   ├── JobService.java                          # Blocking 任务服务（CRUD+批量+查询+重试+统计+生命周期）
│       │   ├── ReactiveJobService.java                  # Reactive 任务服务（Mono/Flux 对齐）
│       │   ├── JobExecutor.java                         # 任务执行器（execute(JobInfo, Context)→JobExecutionLog）
│       │   ├── ReactiveJobExecutor.java                 # 响应式执行器
│       │   ├── JobHandler.java                          # 任务处理器 SPI（supportedJobType + execute）
│       │   ├── JobExecutionListener.java                # 执行监听器
│       │   ├── JobServiceFactory / ReactiveJobServiceFactory
│       │   └── SchedulerManagementService.java          # 调度器生命周期/健康/监听器管理
│       └── util/                                        # 工具类
│           ├── CronUtil.java                            # Cron 表达式工具
│           ├── JobServiceLoader / JobServiceLookup      # Blocking SPI 加载与查找
│           └── ReactiveJobServiceLoader / ReactiveJobServiceLookup
├── scheduler-infrastructure/                            # 实现层（Blocking + Reactive 双实现）
│   └── cn.aryee.scheduler.infrastructure/
│       ├── blocking/                                    # Blocking 实现
│       │   ├── handler/                                 # 任务处理器实现
│       │   │   ├── BeanMethodJobHandler                 # BEAN_METHOD 类型
│       │   │   └── JavaJobHandler                       # JAVA 类型
│       │   ├── quartz/                                  # Quartz 引擎
│       │   │   ├── QuartzJobService + QuartzJobServiceFactory
│       │   │   └── QuartzSchedulerManagementService
│       │   └── xxljob/                                  # XXL-Job 引擎
│       │       ├── XxlJobExecutorConfig                 # 执行器装配
│       │       └── XxlJobService
│       └── reactive/                                    # Reactive 实现
│           ├── quartz/                                  # ReactiveQuartzJobService + Factory
│           └── xxljob/                                  # ReactiveXxlJobService
├── scheduler-spring-boot-autoconfigure/                 # Blocking 自动配置
│   └── 注册: AryeeSchedulerAutoConfiguration / AryeeXxlJobAutoConfiguration
├── scheduler-spring-boot-starter/                       # Blocking Starter 依赖聚合
├── scheduler-reactive-spring-boot-autoconfigure/        # Reactive 自动配置
│   └── 注册: AryeeSchedulerReactiveAutoConfiguration
└── scheduler-reactive-spring-boot-starter/              # Reactive Starter 依赖聚合
```

### 模块说明

| 模块 | artifactId | 说明 |
|------|-----------|------|
| scheduler-api | `scheduler-api` | Blocking + Reactive 契约接口、模型、`SchedulerProperties` 配置、`JobType` 枚举 |
| scheduler-infrastructure | `scheduler-infrastructure` | Quartz / XXL-Job 双引擎的 Blocking + Reactive 实现 |
| scheduler-spring-boot-autoconfigure | `scheduler-spring-boot-autoconfigure` | Blocking 自动配置（`AryeeSchedulerAutoConfiguration` + `AryeeXxlJobAutoConfiguration`） |
| scheduler-spring-boot-starter | `scheduler-spring-boot-starter` | Blocking Starter，含 quartz + xxl-job-core 依赖 |
| scheduler-reactive-spring-boot-autoconfigure | `scheduler-reactive-spring-boot-autoconfigure` | Reactive 自动配置（`AryeeSchedulerReactiveAutoConfiguration`） |
| scheduler-reactive-spring-boot-starter | `scheduler-reactive-spring-boot-starter` | Reactive Starter，全栈响应式 |

## 使用方法

### Maven 依赖

#### Blocking 模式（默认，Servlet / WebMVC 场景）

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>scheduler-spring-boot-starter</artifactId>
</dependency>
```

#### Reactive 模式（WebFlux 场景）

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>scheduler-reactive-spring-boot-starter</artifactId>
</dependency>
```

> - 版本由 BOM 统一管理，无需声明 `<version>`。
> - Blocking 与 Reactive Starter 禁止同时引入。
> - Blocking Starter 已聚合 `quartz` 与 `xxl-job-core` 依赖，按 `aryee.scheduler.type` 装配对应引擎。

### 配置选项

`SchedulerProperties` 配置前缀：`aryee.scheduler`，所有字段均提供默认值，零配置可启动。

```yaml
aryee:
  scheduler:
    enabled: true                       # 是否启用调度模块
    type: quartz                        # 调度引擎：quartz / xxljob / memory

    # XXL-Job 配置（type=xxljob 时生效）
    xxl-job:
      enabled: true
      admin-addresses: http://127.0.0.1:8081/xxl-job-admin   # 调度中心地址（多个逗号分隔）
      access-token:                     # 与调度中心 xxl.job.accessToken 一致
      app-name: aryee-scheduler-executor # 执行器 AppName
      log-path: /data/applogs/xxl-job/jobhandler
      log-retention-days: 30            # -1 表示永久保留
      ip:                               # 为空则自动获取
      port: 9999                        # -1 表示自动获取

    # 线程池配置
    thread-pool:
      pool-size: 10
      thread-priority: 5                # Thread.NORM_PRIORITY
      thread-name-prefix: "aryee-scheduler-"
      daemon: true
      shutdown-grace-period-seconds: 30
      queue-size: 1000
      rejection-policy: CALLER_RUNS     # ABORT / DISCARD / CALLER_RUNS

    # 集群配置
    cluster:
      enabled: false
      node-id:                          # 为空则自动生成
      checkin-interval-ms: 20000
      node-timeout-ms: 60000
      failover-enabled: true
      load-balance-strategy: ROUND_ROBIN # ROUND_ROBIN / RANDOM / LEAST_LOADED / CONSISTENT_HASH

    # 重试配置
    retry:
      default-max-retries: 3
      default-retry-interval-ms: 5000
      default-backoff-type: FIXED       # FIXED / EXPONENTIAL / RANDOM
      default-backoff-multiplier: 2.0
      default-max-retry-interval-ms: 60000

    # 监控配置
    monitoring:
      enabled: true
      stats-retention-days: 30
      health-check-enabled: true
      health-check-interval-seconds: 60
      failure-rate-threshold: 20.0      # 失败率告警阈值（%）
      consecutive-failure-threshold: 3  # 连续失败告警阈值
      record-execution-details: true

    # 日志配置
    log:
      enabled: true
      retention-days: 30
      storage-type: memory              # memory / redis / database
      max-log-size: 10000
      log-input-parameters: true
      log-result-data: true
      log-error-stack: true

    # 存储配置
    storage:
      type: memory                      # memory / redis / database
      persistent: false
      table-prefix: "aryee_scheduler_"

    dependency-enabled: true            # 是否启用 DAG 依赖链
    shard-enabled: true                 # 是否启用任务分片
    tenant-enabled: false               # 是否启用多租户隔离
```

### 任务类型与处理器 SPI

`JobType` 枚举定义五种任务类型，每种类型由对应的 `JobHandler` 实现处理：

| JobType | 说明 | 内置 Handler |
|---------|------|-------------|
| `JAVA` | 实现 `JobExecutor` 接口的 Java 类任务 | `JavaJobHandler` |
| `BEAN_METHOD` | Spring Bean 的指定方法调用 | `BeanMethodJobHandler` |
| `HTTP` | HTTP 调用任务（GET/POST/PUT/DELETE） | — |
| `SCRIPT` | 脚本任务（Shell/Python/Groovy 等） | — |
| `DATA_PIPELINE` | 数据管道任务（ETL） | — |

> 扩展方式：实现 `JobHandler` 接口（`supportedJobType()` + `execute(JobInfo, JobExecutionContext)`）并注册为 Spring Bean，调度器按 `JobInfo.getJobType()` 自动分发。

### Quartz 示例

#### 任务管理（Blocking）

```java
@Service
public class QuartzTaskService {

    private final JobService jobService;

    public QuartzTaskService(JobService jobService) {
        this.jobService = jobService;
    }

    /** 添加 Cron 任务（Java 类任务） */
    public String addJavaJob(String name, String group, String cron, Class<?> jobClass) {
        JobInfo jobInfo = new JobInfo();
        jobInfo.setJobName(name);
        jobInfo.setJobGroup(group);
        jobInfo.setJobType(JobType.JAVA);
        jobInfo.setJobClassName(jobClass.getName());
        jobInfo.setCronExpression(cron);
        return jobService.addJob(jobInfo);
    }

    /** 添加 Bean 方法任务 */
    public String addBeanMethodJob(String name, String cron, String beanName, String method) {
        JobInfo jobInfo = new JobInfo();
        jobInfo.setJobName(name);
        jobInfo.setJobType(JobType.BEAN_METHOD);
        jobInfo.setCronExpression(cron);

        BeanMethodJobConfig config = new BeanMethodJobConfig();
        config.setBeanName(beanName);
        config.setMethodName(method);
        jobInfo.setBeanMethodConfig(config);
        return jobService.addJob(jobInfo);
    }

    /** 控制：暂停 / 恢复 / 立即触发 / 删除 */
    public void pause(String jobId)      { jobService.pauseJob(jobId); }
    public void resume(String jobId)     { jobService.resumeJob(jobId); }
    public void trigger(String jobId)    { jobService.triggerJob(jobId); }
    public void delete(String jobId)     { jobService.deleteJob(jobId); }

    /** 查询与统计 */
    public List<JobInfo> listByGroup(String group) { return jobService.getJobsByGroup(group); }
    public List<JobInfo> listByStatus(JobStatus s) { return jobService.getJobsByStatus(s); }
    public List<JobInfo> search(String keyword)    { return jobService.searchJobs(keyword); }
    public JobExecutionStats stats(String jobId)   { return jobService.getJobStats(jobId); }
    public Map<String, Object> schedulerStats()    { return jobService.getSchedulerStats(); }

    /** 批量操作 */
    public int batchPause(List<String> ids)   { return jobService.batchPauseJobs(ids); }
    public int batchTrigger(List<String> ids) { return jobService.batchTriggerJobs(ids); }

    /** 克隆任务 */
    public String clone(String sourceId, String newName) {
        return jobService.cloneJob(sourceId, newName);
    }
}
```

#### Reactive 模式

```java
@Service
public class ReactiveTaskService {

    private final ReactiveJobService reactiveJobService;

    public ReactiveTaskService(ReactiveJobService reactiveJobService) {
        this.reactiveJobService = reactiveJobService;
    }

    public Mono<String> addJob(JobInfo jobInfo) {
        return reactiveJobService.addJob(jobInfo);
    }

    public Mono<Void> trigger(String jobId) {
        return reactiveJobService.triggerJob(jobId);
    }

    public Flux<JobInfo> listByStatus(JobStatus status) {
        return reactiveJobService.getJobsByStatus(status);
    }

    public Mono<Map<String, Object>> schedulerStats() {
        return reactiveJobService.getSchedulerStats();
    }
}
```

### XXL-Job 示例

#### 定义 `@XxlJob` Handler

```java
@Component
public class SampleXxlJob {

    private static final Logger log = LoggerFactory.getLogger(SampleXxlJob.class);

    /** 简单任务 */
    @XxlJob("sampleJobHandler")
    public void sampleJobHandler() {
        XxlJobHelper.log("XXL-Job sample job handler.");
    }

    /** 分片广播任务 */
    @XxlJob("shardingJobHandler")
    public void shardingJobHandler() {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        XxlJobHelper.log("分片：当前={}, 总数={}", shardIndex, shardTotal);
    }
}
```

#### 切换到 XXL-Job 引擎

```yaml
aryee:
  scheduler:
    type: xxljob
    xxl-job:
      admin-addresses: http://127.0.0.1:8081/xxl-job-admin
      app-name: aryee-scheduler-executor
      access-token: default_token
      port: 9999
```

切换后 `JobService`（Blocking）或 `ReactiveJobService`（Reactive）自动注入 XXL-Job 实现。

### 调度器管理示例

```java
@Service
public class SchedulerManageService {

    private final SchedulerManagementService managementService;

    public SchedulerManageService(SchedulerManagementService managementService) {
        this.managementService = managementService;
    }

    /** 生命周期 */
    public void start()        { managementService.start(); }
    public void shutdown()     { managementService.shutdown(); }
    public void pauseAll()     { managementService.pauseAll(); }
    public void resumeAll()    { managementService.resumeAll(); }

    /** 健康检查 */
    public Map<String, Object> health() { return managementService.getHealth(); }
    public boolean isHealthy()          { return managementService.isHealthy(); }

    /** 集群信息 */
    public String instanceId()          { return managementService.getSchedulerInstanceId(); }
    public Map<String, Object> metadata() { return managementService.getMetadata(); }

    /** 监听器管理 */
    public void addListener(JobExecutionListener listener)   { managementService.addListener(listener); }
    public void removeListener(JobExecutionListener listener) { managementService.removeListener(listener); }
}
```

### 重试策略示例

```java
@Service
public class RetryPolicyService {

    private final JobService jobService;

    public RetryPolicyService(JobService jobService) {
        this.jobService = jobService;
    }

    /** 为任务设置指数退避重试策略 */
    public void setExponentialRetry(String jobId) {
        RetryPolicy policy = new RetryPolicy();
        policy.setMaxRetries(5);
        policy.setRetryIntervalMs(2000);
        policy.setBackoffType(RetryPolicy.BackoffType.EXPONENTIAL);
        policy.setBackoffMultiplier(2.0);
        policy.setMaxRetryIntervalMs(60000);
        jobService.setRetryPolicy(jobId, policy);
    }
}
```

## XXL-Job 实现说明

本模块的 XXL-Job 实现遵循「执行器注册 + 本地元数据管理」模式：

1. **执行器注册**: `XxlJobExecutorConfig` 在 `aryee.scheduler.type=xxljob` 且类路径存在 `xxl-job-core` 时条件装配 `XxlJobSpringExecutor`，自动扫描 Spring 容器中所有 `@XxlJob` 注解方法并注册到 admin 调度中心。
2. **调度控制**: Cron 触发、失败转移、分片广播、暂停 / 恢复等调度策略由 admin 调度中心集中管理，`XxlJobService` 的 `pauseJob` / `resumeJob` / `triggerJob` 同步本地元数据状态。
3. **元数据管理**: `XxlJobService` 维护本地任务信息、执行日志与统计监控视图，满足 `JobService` 契约的查询与统计能力。
4. **响应式支持**: `ReactiveXxlJobService` 包装阻塞 `XxlJobService`，所有调用通过 `Schedulers.boundedElastic()` 调度到弹性线程池，保证 Reactive 流水线非阻塞。

## 安全管控

调度模块支持可选的安全管控能力，委托 [security 模块](security.md) 进行任务操作权限检查和审计日志，遵循 [security-governance.md](https://github.com/aryeecn/aryee-foundation)（内部规范：security-governance） 规则。

### 工作原理

```
调用方 → SecuredJobService（装饰器） → 原始 JobService
              ↓                              ↓
     SchedulerSecurityService           任务增删改查/触发
      ├─ checkPermission()              （委托 security 模块）
      └─ audit()
```

### 安全风险等级

| 操作 | 风险等级 | 权限常量 |
|------|---------|---------|
| `triggerJob()` | 🔴 最高 | `scheduler:execute` |
| `batchTriggerJobs()` | 🔴 最高 | `scheduler:execute` |
| `deleteJob()` | 🟠 高 | `scheduler:delete` |
| `pauseJob()` / `resumeJob()` | 🟡 中 | `scheduler:control` |
| `addJob()` / `updateJob()` | 🟡 中 | `scheduler:create` / `scheduler:update` |
| `getJob()` / `getAllJobs()` | 🟢 低 | 仅查询，不检查写权限 |

### 配置示例

```yaml
aryee:
  scheduler:
    security:
      enabled: true                # 启用安全管控
      audit-enabled: true          # 启用操作审计日志
      trigger-rate-limit: true     # 启用任务触发限流
      max-triggers-per-minute: 10  # 单用户每分钟最大触发次数
```

### 安全装饰器

模块通过装饰器模式为所有 `JobService` / `ReactiveJobService` 操作注入权限检查与审计日志，无需侵入业务代码：

- **`SecuredJobService`**（Blocking）: 装饰原始 `JobService`，对 `addJob` / `updateJob` / `deleteJob` / `triggerJob` / `pauseJob` / `resumeJob` 等所有写操作执行权限检查与审计日志记录
- **`SecuredReactiveJobService`**（Reactive）: 装饰原始 `ReactiveJobService`，在响应式流水线中通过 `flatMap` / `doOnSuccess` 等操作符非阻塞地完成权限检查与审计

```
调用方 → SecuredJobService / SecuredReactiveJobService → 原始服务实现
              ↓
     SchedulerSecurityService
      ├─ checkPermission()  → 委托 security 模块权限校验
      └─ audit()            → 记录操作审计日志
```

### 异常处理策略

`QuartzJobService`、`ReactiveQuartzJobService`、`QuartzSchedulerManagementService` 内部的所有 `RuntimeException` 已统一替换为 `SystemException`：

- `SystemException` 继承自 `RuntimeException`，携带错误码与国际化消息
- 上层调用方通过 `try-catch` 或全局异常处理器可统一捕获
- 保持 API 兼容性，无需修改现有调用代码的异常处理逻辑

### 默认调度用户

调度任务的默认执行用户为 **`system`**，通过 `SecurityContextHolder` 管理：

```java
// 调度框架内部执行时自动设置为 "system" 用户
SecurityContextHolder.setUserId("system");
try {
    // Quartz / XXL-Job 触发的任务在此安全上下文中执行
    jobService.triggerJob(jobId);
} finally {
    SecurityContextHolder.clear();
}
```

- 调度器自动触发的任务（Cron / 手动触发）统一以 `system` 用户身份执行
- 通过 `SecurityContextHolder.setUserId()` 可在业务操作前切换为当前操作用户
- `system` 用户拥有完整的调度操作权限，确保系统级任务不受业务权限限制

### 使用方式

```java
// 在 Controller 或 Filter 中设置当前用户
SecurityContextHolder.setUserId(currentUserId);
try {
    jobService.triggerJob(jobId); // 自动权限检查 + 审计日志
} finally {
    SecurityContextHolder.clear();
}
```

### 条件装配

| Bean | 条件 | 说明 |
|------|------|------|
| `DefaultSchedulerSecurityService` | `DynamicPermissionService` + `SecurityAuditService` Bean 存在 + `security.enabled=true` | 委托 security 模块 |
| `NoopSchedulerSecurityService` | `security.enabled=true` 但 security 模块未引入 | 降级方案 |
| `SecuredJobService` (Blocking) | `SchedulerSecurityService` Bean 存在 | `@Primary` 装饰器 |
| `SecuredReactiveJobService` (Reactive) | 同上 | `@Primary` 装饰器 |

## 兼容性

### 运行环境

| 项 | 版本 |
|----|------|
| Java | 21+ |
| Spring Boot | 4.0.7 |
| Quartz | 跟随 Spring Boot 版本 |
| XXL-Job | xxl-job-core |

### 调度引擎矩阵

| 引擎 | Blocking 实现 | Reactive 实现 | 默认 | 适用场景 |
|------|--------------|--------------|------|---------|
| Quartz | `QuartzJobService` | `ReactiveQuartzJobService` | ✅ | 单体 / 简单分布式、精细控制 |
| XXL-Job | `XxlJobService` | `ReactiveXxlJobService` | — | 微服务、可视化调度、分片广播 |
| Memory | — | — | — | 开发测试（`type=memory` 占位） |

### 调度器管理实现

| 实现 | 说明 |
|------|------|
| `QuartzSchedulerManagementService` | Blocking 模式调度器生命周期 / 健康 / 监听器管理 |
