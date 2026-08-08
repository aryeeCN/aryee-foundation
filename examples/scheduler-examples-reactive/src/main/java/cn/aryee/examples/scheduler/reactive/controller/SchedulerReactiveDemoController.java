package cn.aryee.examples.scheduler.reactive.controller;

import cn.aryee.examples.scheduler.reactive.service.SchedulerReactiveDemoService;
import cn.aryee.scheduler.api.model.JobExecutionLog;
import cn.aryee.scheduler.api.model.JobInfo;
import cn.aryee.scheduler.api.service.ReactiveJobService;
import cn.aryee.scheduler.api.util.CronUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Scheduler Reactive 模块功能演示 Controller
 * 演示任务创建、管理、执行等核心能力，返回 Mono/Flux
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/scheduler")
@RequiredArgsConstructor
public class SchedulerReactiveDemoController {

    private final SchedulerReactiveDemoService demoService;
    private final ReactiveJobService reactiveJobService;

    // ========== 任务创建 ==========

    /**
     * 创建 BEAN_METHOD 任务（禁止并发）
     */
    @PostMapping("/jobs/bean-method")
    public Mono<Map<String, Object>> createBeanMethodJob(
            @RequestParam String jobName,
            @RequestParam(defaultValue = "0/5 * * * * ?") String cron,
            @RequestParam(defaultValue = "sampleJobTasks") String beanName,
            @RequestParam(defaultValue = "syncData") String methodName) {
        log.info("[创建任务] BEAN_METHOD: name={}, cron={}.{}={}", jobName, cron, beanName, methodName);
        return demoService.createBeanMethodJob(jobName, cron, beanName, methodName)
                .map(jobId -> Map.<String, Object>of("jobId", jobId, "message", "任务创建成功"));
    }

    /**
     * 创建带参数的 BEAN_METHOD 任务
     */
    @PostMapping("/jobs/bean-method-args")
    public Mono<Map<String, Object>> createBeanMethodWithArgs(
            @RequestParam String jobName,
            @RequestParam(defaultValue = "0/10 * * * * ?") String cron,
            @RequestParam(defaultValue = "sampleJobTasks") String beanName,
            @RequestParam(defaultValue = "processOrder") String methodName,
            @RequestParam String orderId,
            @RequestParam(defaultValue = "0") int retryCount) {
        log.info("[创建任务] BEAN_METHOD(带参): name={}, {}.{}({})", jobName, beanName, methodName, orderId);
        return demoService.createBeanMethodWithArgs(jobName, cron, beanName, methodName,
                new Object[]{orderId, retryCount})
                .map(jobId -> Map.<String, Object>of("jobId", jobId, "message", "带参任务创建成功"));
    }

    /**
     * 创建 HTTP 调用任务
     */
    @PostMapping("/jobs/http")
    public Mono<Map<String, Object>> createHttpJob(
            @RequestParam String jobName,
            @RequestParam(defaultValue = "0/30 * * * * ?") String cron,
            @RequestParam String url,
            @RequestParam(defaultValue = "GET") String method) {
        log.info("[创建任务] HTTP: name={}, cron={}, url={}", jobName, cron, url);
        return demoService.createHttpJob(jobName, cron, url, method)
                .map(jobId -> Map.<String, Object>of("jobId", jobId, "message", "HTTP任务创建成功"));
    }

    /**
     * 创建带重试策略的任务
     */
    @PostMapping("/jobs/retry")
    public Mono<Map<String, Object>> createRetryJob(
            @RequestParam String jobName,
            @RequestParam(defaultValue = "0/20 * * * * ?") String cron,
            @RequestParam(defaultValue = "sampleJobTasks") String beanName,
            @RequestParam(defaultValue = "unstableTask") String methodName,
            @RequestParam(defaultValue = "3") int maxRetries,
            @RequestParam(defaultValue = "2000") long retryIntervalMs) {
        log.info("[创建任务] Retry: name={}, maxRetries={}", jobName, maxRetries);
        return demoService.createJobWithRetry(jobName, cron, beanName, methodName, maxRetries, retryIntervalMs)
                .map(jobId -> Map.<String, Object>of("jobId", jobId, "message", "重试任务创建成功"));
    }

    /**
     * 创建带超时的任务
     */
    @PostMapping("/jobs/timeout")
    public Mono<Map<String, Object>> createTimeoutJob(
            @RequestParam String jobName,
            @RequestParam(defaultValue = "0/15 * * * * ?") String cron,
            @RequestParam(defaultValue = "sampleJobTasks") String beanName,
            @RequestParam(defaultValue = "longRunningTask") String methodName,
            @RequestParam(defaultValue = "3000") long timeoutMs) {
        log.info("[创建任务] Timeout: name={}, timeoutMs={}", jobName, timeoutMs);
        return demoService.createJobWithTimeout(jobName, cron, beanName, methodName, timeoutMs)
                .map(jobId -> Map.<String, Object>of("jobId", jobId, "message", "超时任务创建成功"));
    }

    // ========== 任务管理 ==========

    /**
     * 查询所有任务
     */
    @GetMapping("/jobs")
    public Flux<JobInfo> listJobs() {
        return demoService.listJobs();
    }

    /**
     * 查询任务详情
     */
    @GetMapping("/jobs/{jobId}")
    public Mono<JobInfo> getJob(@PathVariable String jobId) {
        return reactiveJobService.getJob(jobId);
    }

    /**
     * 暂停任务
     */
    @PutMapping("/jobs/{jobId}/pause")
    public Mono<Map<String, Object>> pauseJob(@PathVariable String jobId) {
        log.info("[暂停任务] jobId={}", jobId);
        return reactiveJobService.pauseJob(jobId)
                .then(Mono.just(Map.<String, Object>of("message", "任务已暂停")));
    }

    /**
     * 恢复任务
     */
    @PutMapping("/jobs/{jobId}/resume")
    public Mono<Map<String, Object>> resumeJob(@PathVariable String jobId) {
        log.info("[恢复任务] jobId={}", jobId);
        return reactiveJobService.resumeJob(jobId)
                .then(Mono.just(Map.<String, Object>of("message", "任务已恢复")));
    }

    /**
     * 立即触发任务
     */
    @PostMapping("/jobs/{jobId}/trigger")
    public Mono<Map<String, Object>> triggerJob(@PathVariable String jobId) {
        log.info("[触发任务] jobId={}", jobId);
        return reactiveJobService.triggerJob(jobId)
                .then(Mono.just(Map.<String, Object>of("message", "任务已触发")));
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/jobs/{jobId}")
    public Mono<Map<String, Object>> deleteJob(@PathVariable String jobId) {
        log.info("[删除任务] jobId={}", jobId);
        return reactiveJobService.deleteJob(jobId)
                .then(Mono.just(Map.<String, Object>of("message", "任务已删除")));
    }

    // ========== 执行日志 & 统计 ==========

    /**
     * 查询任务执行日志
     */
    @GetMapping("/jobs/{jobId}/logs")
    public Flux<JobExecutionLog> getJobLogs(@PathVariable String jobId) {
        return demoService.getJobLogs(jobId);
    }

    /**
     * 获取调度器全局统计
     */
    @GetMapping("/stats")
    public Mono<Map<String, Object>> getStats() {
        return demoService.getSchedulerStats();
    }

    // ========== 工具 ==========

    /**
     * 验证 Cron 表达式
     */
    @GetMapping("/cron/validate")
    public Mono<Map<String, Object>> validateCron(@RequestParam String cron) {
        boolean valid = CronUtil.isValid(cron);
        return Mono.just(Map.of("cron", cron, "valid", valid));
    }
}
