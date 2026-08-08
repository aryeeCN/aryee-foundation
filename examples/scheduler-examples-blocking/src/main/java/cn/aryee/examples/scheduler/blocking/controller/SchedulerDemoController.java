package cn.aryee.examples.scheduler.blocking.controller;

import cn.aryee.examples.scheduler.blocking.service.SchedulerDemoService;
import cn.aryee.scheduler.api.model.JobExecutionLog;
import cn.aryee.scheduler.api.model.JobInfo;
import cn.aryee.scheduler.api.service.JobService;
import cn.aryee.scheduler.api.util.CronUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Scheduler 模块功能演示 Controller
 * 演示任务创建、管理、执行等核心能力
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/scheduler")
@RequiredArgsConstructor
public class SchedulerDemoController {

    private final SchedulerDemoService demoService;
    private final JobService jobService;

    // ========== 任务创建 ==========

    /**
     * 创建 BEAN_METHOD 任务（禁止并发）
     * 示例：每 5 秒执行一次数据同步
     */
    @PostMapping("/jobs/bean-method")
    public Map<String, Object> createBeanMethodJob(
            @RequestParam String jobName,
            @RequestParam(defaultValue = "0/5 * * * * ?") String cron,
            @RequestParam(defaultValue = "sampleJobTasks") String beanName,
            @RequestParam(defaultValue = "syncData") String methodName) {
        log.info("[创建任务] BEAN_METHOD: name={}, cron={}.{}={}", jobName, cron, beanName, methodName);
        String jobId = demoService.createBeanMethodJob(jobName, cron, beanName, methodName);
        return Map.of("jobId", jobId, "message", "任务创建成功");
    }

    /**
     * 创建带参数的 BEAN_METHOD 任务
     */
    @PostMapping("/jobs/bean-method-args")
    public Map<String, Object> createBeanMethodWithArgs(
            @RequestParam String jobName,
            @RequestParam(defaultValue = "0/10 * * * * ?") String cron,
            @RequestParam(defaultValue = "sampleJobTasks") String beanName,
            @RequestParam(defaultValue = "processOrder") String methodName,
            @RequestParam String orderId,
            @RequestParam(defaultValue = "0") int retryCount) {
        log.info("[创建任务] BEAN_METHOD(带参): name={}, {}.{}({})", jobName, beanName, methodName, orderId);
        String jobId = demoService.createBeanMethodWithArgs(jobName, cron, beanName, methodName,
                new Object[]{orderId, retryCount});
        return Map.of("jobId", jobId, "message", "带参任务创建成功");
    }

    /**
     * 创建 HTTP 调用任务
     */
    @PostMapping("/jobs/http")
    public Map<String, Object> createHttpJob(
            @RequestParam String jobName,
            @RequestParam(defaultValue = "0/30 * * * * ?") String cron,
            @RequestParam String url,
            @RequestParam(defaultValue = "GET") String method) {
        log.info("[创建任务] HTTP: name={}, cron={}, url={}", jobName, cron, url);
        String jobId = demoService.createHttpJob(jobName, cron, url, method);
        return Map.of("jobId", jobId, "message", "HTTP任务创建成功");
    }

    /**
     * 创建带重试策略的任务
     */
    @PostMapping("/jobs/retry")
    public Map<String, Object> createRetryJob(
            @RequestParam String jobName,
            @RequestParam(defaultValue = "0/20 * * * * ?") String cron,
            @RequestParam(defaultValue = "sampleJobTasks") String beanName,
            @RequestParam(defaultValue = "unstableTask") String methodName,
            @RequestParam(defaultValue = "3") int maxRetries,
            @RequestParam(defaultValue = "2000") long retryIntervalMs) {
        log.info("[创建任务] Retry: name={}, maxRetries={}", jobName, maxRetries);
        String jobId = demoService.createJobWithRetry(jobName, cron, beanName, methodName,
                maxRetries, retryIntervalMs);
        return Map.of("jobId", jobId, "message", "重试任务创建成功");
    }

    /**
     * 创建带超时的任务
     */
    @PostMapping("/jobs/timeout")
    public Map<String, Object> createTimeoutJob(
            @RequestParam String jobName,
            @RequestParam(defaultValue = "0/15 * * * * ?") String cron,
            @RequestParam(defaultValue = "sampleJobTasks") String beanName,
            @RequestParam(defaultValue = "longRunningTask") String methodName,
            @RequestParam(defaultValue = "3000") long timeoutMs) {
        log.info("[创建任务] Timeout: name={}, timeoutMs={}", jobName, timeoutMs);
        String jobId = demoService.createJobWithTimeout(jobName, cron, beanName, methodName, timeoutMs);
        return Map.of("jobId", jobId, "message", "超时任务创建成功");
    }

    // ========== 任务管理 ==========

    /**
     * 查询所有任务
     */
    @GetMapping("/jobs")
    public List<JobInfo> listJobs() {
        return demoService.listJobs();
    }

    /**
     * 查询任务详情
     */
    @GetMapping("/jobs/{jobId}")
    public JobInfo getJob(@PathVariable String jobId) {
        return jobService.getJob(jobId);
    }

    /**
     * 暂停任务
     */
    @PutMapping("/jobs/{jobId}/pause")
    public Map<String, Object> pauseJob(@PathVariable String jobId) {
        log.info("[暂停任务] jobId={}", jobId);
        jobService.pauseJob(jobId);
        return Map.of("message", "任务已暂停");
    }

    /**
     * 恢复任务
     */
    @PutMapping("/jobs/{jobId}/resume")
    public Map<String, Object> resumeJob(@PathVariable String jobId) {
        log.info("[恢复任务] jobId={}", jobId);
        jobService.resumeJob(jobId);
        return Map.of("message", "任务已恢复");
    }

    /**
     * 立即触发任务
     */
    @PostMapping("/jobs/{jobId}/trigger")
    public Map<String, Object> triggerJob(@PathVariable String jobId) {
        log.info("[触发任务] jobId={}", jobId);
        jobService.triggerJob(jobId);
        return Map.of("message", "任务已触发");
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/jobs/{jobId}")
    public Map<String, Object> deleteJob(@PathVariable String jobId) {
        log.info("[删除任务] jobId={}", jobId);
        jobService.deleteJob(jobId);
        return Map.of("message", "任务已删除");
    }

    // ========== 执行日志 & 统计 ==========

    /**
     * 查询任务执行日志
     */
    @GetMapping("/jobs/{jobId}/logs")
    public List<JobExecutionLog> getJobLogs(@PathVariable String jobId) {
        return demoService.getJobLogs(jobId);
    }

    /**
     * 获取调度器全局统计
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return demoService.getSchedulerStats();
    }

    // ========== 工具 ==========

    /**
     * 验证 Cron 表达式
     */
    @GetMapping("/cron/validate")
    public Map<String, Object> validateCron(@RequestParam String cron) {
        boolean valid = CronUtil.isValid(cron);
        return Map.of("cron", cron, "valid", valid);
    }
}
