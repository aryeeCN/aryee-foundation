# Aryee Foundation Workflow

基于 [Flowable 7.2.0](https://www.flowable.com/) 的 BPMN 工作流引擎模块，提供流程定义部署、流程实例管理、用户任务办理等能力，同时支持 **Blocking 模式**与 **Reactive 模式**双编程模型。

## 模块结构

```
aryee-foundation-workflow/
├── workflow-api/                        # API 层：接口契约、模型、枚举、异常、配置属性
│   ├── service/WorkflowService.java     # Blocking 服务接口
│   ├── service/ReactiveWorkflowService.java  # Reactive 服务接口
│   ├── service/WorkflowTaskService.java       # Blocking 任务接口
│   ├── service/ReactiveWorkflowTaskService.java   # Reactive 任务接口
│   ├── service/ProcessDeploymentService.java   # Blocking 部署接口
│   └── service/ReactiveProcessDeploymentService.java # Reactive 部署接口
├── workflow-infrastructure/             # 基础设施层：基于 Flowable 的 Blocking 实现
│   └── blocking/flowable/               # Flowable 实现（Blocking 模式）
├── workflow-spring-boot-autoconfigure/  # 自动配置层（Blocking + Reactive 双装配）
└── workflow-spring-boot-starter/        # Starter 层（依赖聚合）

> Reactive 模式通过 `Mono.fromCallable().subscribeOn(Schedulers.boundedElastic())` 将阻塞的 Flowable 调用包装为非阻塞，确保在响应式流水线中不阻塞事件循环线程。
```

## 引入方式

### 1. Blocking Starter（WebMVC 场景）

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>workflow-spring-boot-starter</artifactId>
</dependency>
```

### 2. Reactive Starter（WebFlux 场景）

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>workflow-reactive-spring-boot-starter</artifactId>
</dependency>
```

## 配置项

所有配置项使用 `aryee.workflow` 前缀：

```yaml
aryee:
  workflow:
    enabled: true                              # 是否启用工作流模块（默认 true）
    type: flowable                              # 实现类型（目前仅支持 flowable）
    service-name: aryee-workflow                # 服务名
    auto-deploy: false                          # 是否自动部署 classpath:/processes/ 下的 BPMN 文件
    database-schema-update: true                # Flowable 建表策略（true=自动创建/更新表结构）
    history-level: full                         # 历史记录级别：none/activity/audit/full
    deployment-resources: classpath*:/processes/  # BPMN 资源路径
    database:
      schema-update: true                       # 建表策略
      db-history-used: true                     # 是否启用历史记录
    tenant:
      auto-fill: true                           # 部署/启动时自动从租户上下文填充 tenantId（需 tenant 模块）
    event:
      enabled: true                             # 是否启用工作流事件监听
    audit:
      enabled: true                             # 是否启用操作审计（需 security 模块）
    rest:
      enabled: true                             # 是否启用 REST 管理 API（仅 WebMVC）
      base-path: /aryee/workflow                # REST 基础路径
```

## 核心服务

### WorkflowService

流程实例管理服务，提供：
- `startProcess(ProcessStartRequest)` - 启动流程实例
- `getProcessInstance(processInstanceId)` - 查询流程实例
- `terminateProcess(processInstanceId, reason)` - 终止流程实例
- `suspendProcess / activateProcess` - 挂起/激活流程实例
- `queryProcessInstances / countProcessInstances` - 分页查询流程实例
- `setProcessVariables / getProcessVariables` - 流程变量管理
- `getCurrentActivityIds(processInstanceId)` - 获取当前活动节点（流程追踪）
- `getActivityTrace(processInstanceId)` - 获取执行轨迹（经过的所有节点）
- `queryFinishedProcessInstances / countFinishedProcessInstances` - 分页查询已办结流程

### WorkflowTaskService

用户任务管理服务，提供：
- `getMyTasks(userId, pageNum, pageSize)` - 查询我的待办任务
- `getTasksByProcessInstance(processInstanceId)` - 按流程实例查询任务
- `completeTask(taskId, variables)` - 完成任务
- `completeTask(taskId, variables, comment, userId)` - 完成任务并记录审批意见
- `getApprovalRecords(processInstanceId)` - 查询审批记录（含审批意见，按时间升序）
- `getMyHistoryTasks / countMyHistoryTasks` - 查询我已办结的历史任务
- `getOverdueTasks / countOverdueTasks` - 查询已超时的待办任务
- `claimTask(taskId, userId)` - 签收任务
- `delegateTask(taskId, userId)` - 委派任务
- `resolveTask(taskId, variables)` - 解决委派任务
- `returnTask(taskId, targetActivityId)` - 退回任务到指定节点

### ProcessDeploymentService

流程部署管理服务，提供：
- `deploy(resourceName, bpmnStream, tenantId)` - 部署 BPMN 流程
- `undeploy(deploymentId)` - 卸载部署
- `listProcessDefinitions(key, latest)` - 查询流程定义列表
- `getProcessDefinition(processDefinitionId)` - 获取流程定义
- `getLatestProcessDefinition(key)` - 获取最新版本流程定义

### Reactive 服务接口

Reactive 模式提供同名响应式接口，通过 Reactor 的 `Mono`/`Flux` 返回：

- `ReactiveWorkflowService` - 响应式流程实例管理
- `ReactiveWorkflowTaskService` - 响应式用户任务管理
- `ReactiveProcessDeploymentService` - 响应式流程部署管理

> 所有阻塞的 Flowable 调用通过 `Mono.fromCallable().subscribeOn(Schedulers.boundedElastic())` 包装为非阻塞调用。

## 增强能力（自动装配）

### 多租户自动填充

引入 `tenant-spring-boot-starter` 后，部署/启动流程未显式指定 tenantId 时，
自动从 `TenantContextHolder` 填充当前租户（`aryee.workflow.tenant.auto-fill` 控制，默认开启）。
也可自定义 `WorkflowTenantProvider` Bean 覆盖默认实现。

### 事件通知

实现 `WorkflowEventListener` 并注册为 Spring Bean，即可接收流程/任务生命周期事件
（`onProcessStarted` / `onProcessTerminated` / `onTaskCreated` / `onTaskCompleted`），
典型场景：任务创建时推送待办提醒（钉钉/飞书/站内信）。单个监听器异常不影响主流程。

```java
@Component
public class TodoNotifyListener implements WorkflowEventListener {
    @Override
    public void onTaskCreated(TaskInfo task) {
        // 推送待办提醒
    }
}
```

### 安全审计

引入 `security-spring-boot-starter` 后，流程/任务/部署的写操作自动委托
`SecurityAuditService` 记录审计日志（`aryee.workflow.audit.enabled` 控制，默认开启），
审计失败不影响业务。

### REST 管理 API（仅 WebMVC）

WebMVC 环境下自动注册管理端点（基础路径 `aryee.workflow.rest.base-path`，默认 `/aryee/workflow`），
生产环境请自行在网关/安全层添加管理员鉴权：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/process-definitions` | 查询流程定义列表 |
| POST | `/deployments` | 部署 BPMN（multipart） |
| DELETE | `/deployments/{id}` | 卸载部署 |
| POST | `/process-instances` | 启动流程实例 |
| GET | `/process-instances` | 分页查询流程实例 |
| GET | `/process-instances/{id}` | 流程实例详情（含当前节点） |
| DELETE | `/process-instances/{id}` | 终止流程 |
| GET | `/process-instances/{id}/trace` | 执行轨迹 |
| GET | `/process-instances/{id}/approval-records` | 审批记录 |
| GET | `/process-instances/finished` | 已办结流程 |
| GET | `/tasks` | 我的待办任务 |
| POST | `/tasks/{id}/complete` | 完成任务（含审批意见） |
| GET | `/tasks/history` | 历史任务 |
| GET | `/tasks/overdue` | 超时任务 |

## 使用示例

```java
@Service
@RequiredArgsConstructor
public class LeaveService {

    private final WorkflowService workflowService;
    private final WorkflowTaskService workflowTaskService;

    /**
     * 发起请假流程
     */
    public ProcessInstanceInfo startLeave(String userId, int days, String reason) {
        ProcessStartRequest request = ProcessStartRequest.builder()
                .processDefinitionKey("leave-process")
                .businessKey("LEAVE-" + System.currentTimeMillis())
                .startUserId(userId)
                .name("请假申请-" + userId)
                .variables(Map.of("days", days, "reason", reason))
                .build();
        return workflowService.startProcess(request);
    }

    /**
     * 完成审批任务
     */
    public boolean approve(String taskId, boolean approved) {
        return workflowTaskService.completeTask(taskId, Map.of("approved", approved));
    }

    /**
     * 查询我的待办任务
     */
    public List<TaskInfo> myTasks(String userId, int pageNum, int pageSize) {
        return workflowTaskService.getMyTasks(userId, pageNum, pageSize);
    }
}
```

## 错误码

错误码范围：27000-27999（WORKFLOW 段）

| 错误码 | 枚举值 | 说明 |
|--------|--------|------|
| 27001 | PROCESS_DEFINITION_NOT_FOUND | 流程定义不存在 |
| 27002 | PROCESS_INSTANCE_NOT_FOUND | 流程实例不存在 |
| 27003 | TASK_NOT_FOUND | 任务不存在 |
| 27004 | PROCESS_START_FAILED | 流程启动失败 |
| 27005 | TASK_COMPLETE_FAILED | 任务完成失败 |
| 27006 | DEPLOYMENT_FAILED | 流程部署失败 |
| 27007 | PROCESS_DEFINITION_ALREADY_EXISTS | 流程定义已存在 |
| 27008 | INVALID_PROCESS_DEFINITION | 无效的流程定义 |
| 27009 | PROCESS_INSTANCE_SUSPENDED | 流程实例已挂起 |
| 27010 | TASK_ALREADY_COMPLETED | 任务已完成 |
| 27011 | TRACE_QUERY_FAILED | 流程追踪查询失败 |
| 27012 | APPROVAL_RECORD_FAILED | 审批意见记录失败 |

## 依赖

- Flowable 7.2.0（BPMN 工作流引擎）
- Spring Boot 4.0.x
- Java 21+