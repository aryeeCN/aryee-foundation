package cn.aryee.examples.scheduler.reactive.service;

import cn.aryee.scheduler.api.enums.JobType;
import cn.aryee.scheduler.api.model.*;
import cn.aryee.scheduler.api.service.ReactiveJobService;
import cn.aryee.scheduler.infrastructure.util.CronUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Scheduler Reactive 演示服务
 * 封装各种任务类型的创建逻辑，返回 Mono/Flux
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerReactiveDemoService {

    private final ReactiveJobService reactiveJobService;

    /**
     * 创建 BEAN_METHOD 类型任务（禁止并发）
     */
    public Mono<String> createBeanMethodJob(String jobName, String cron, String beanName, String methodName) {
        return Mono.fromCallable(() -> {
            validateCron(cron);
            JobInfo jobInfo = new JobInfo();
            jobInfo.setJobName(jobName);
            jobInfo.setJobGroup("demo");
            jobInfo.setJobType(JobType.BEAN_METHOD);
            jobInfo.setCronExpression(cron);
            jobInfo.setConcurrent(false);
            jobInfo.setDurable(true);
            jobInfo.setRequestsRecovery(true);
            jobInfo.setBeanMethodConfig(BeanMethodJobConfig.of(beanName, methodName));
            return jobInfo;
        }).flatMap(reactiveJobService::addJob);
    }

    /**
     * 创建带参数的 BEAN_METHOD 任务
     */
    public Mono<String> createBeanMethodWithArgs(String jobName, String cron,
                                                  String beanName, String methodName, Object[] args) {
        return Mono.fromCallable(() -> {
            validateCron(cron);
            JobInfo jobInfo = new JobInfo();
            jobInfo.setJobName(jobName);
            jobInfo.setJobGroup("demo");
            jobInfo.setJobType(JobType.BEAN_METHOD);
            jobInfo.setCronExpression(cron);
            jobInfo.setConcurrent(true);
            jobInfo.setBeanMethodConfig(BeanMethodJobConfig.of(beanName, methodName, args));
            return jobInfo;
        }).flatMap(reactiveJobService::addJob);
    }

    /**
     * 创建 HTTP 调用任务
     */
    public Mono<String> createHttpJob(String jobName, String cron, String url, String method) {
        return Mono.fromCallable(() -> {
            validateCron(cron);
            JobInfo jobInfo = new JobInfo();
            jobInfo.setJobName(jobName);
            jobInfo.setJobGroup("demo");
            jobInfo.setJobType(JobType.HTTP);
            jobInfo.setCronExpression(cron);
            jobInfo.setConcurrent(true);

            HttpJobConfig httpConfig = HttpJobConfig.get(url);
            if (method != null && !method.isBlank()) {
                httpConfig.setMethod(method.toUpperCase());
            }
            httpConfig.setConnectTimeoutMs(5000);
            httpConfig.setReadTimeoutMs(15000);
            jobInfo.setHttpConfig(httpConfig);
            return jobInfo;
        }).flatMap(reactiveJobService::addJob);
    }

    /**
     * 创建带重试策略的任务
     */
    public Mono<String> createJobWithRetry(String jobName, String cron, String beanName, String methodName,
                                            int maxRetries, long retryIntervalMs) {
        return Mono.fromCallable(() -> {
            validateCron(cron);
            JobInfo jobInfo = new JobInfo();
            jobInfo.setJobName(jobName);
            jobInfo.setJobGroup("demo-retry");
            jobInfo.setJobType(JobType.BEAN_METHOD);
            jobInfo.setCronExpression(cron);
            jobInfo.setConcurrent(false);
            jobInfo.setBeanMethodConfig(BeanMethodJobConfig.of(beanName, methodName));

            RetryPolicy retryPolicy = RetryPolicy.exponentialBackoff(maxRetries, retryIntervalMs, 2.0, 60000);
            jobInfo.setRetryPolicy(retryPolicy);
            return jobInfo;
        }).flatMap(reactiveJobService::addJob);
    }

    /**
     * 创建带超时的任务
     */
    public Mono<String> createJobWithTimeout(String jobName, String cron, String beanName, String methodName,
                                              long timeoutMs) {
        return Mono.fromCallable(() -> {
            validateCron(cron);
            JobInfo jobInfo = new JobInfo();
            jobInfo.setJobName(jobName);
            jobInfo.setJobGroup("demo-timeout");
            jobInfo.setJobType(JobType.BEAN_METHOD);
            jobInfo.setCronExpression(cron);
            jobInfo.setConcurrent(false);
            jobInfo.setTimeoutMs(timeoutMs);
            jobInfo.setBeanMethodConfig(BeanMethodJobConfig.of(beanName, methodName));
            return jobInfo;
        }).flatMap(reactiveJobService::addJob);
    }

    /**
     * 获取所有任务
     */
    public Flux<JobInfo> listJobs() {
        return reactiveJobService.getAllJobs();
    }

    /**
     * 获取任务执行日志
     */
    public Flux<JobExecutionLog> getJobLogs(String jobId) {
        return reactiveJobService.getJobExecutionLogs(jobId, 10, 1);
    }

    /**
     * 获取调度器统计
     */
    public Mono<Map<String, Object>> getSchedulerStats() {
        return reactiveJobService.getSchedulerStats();
    }

    private void validateCron(String cron) {
        if (!CronUtil.isValid(cron)) {
            throw new IllegalArgumentException("无效的 Cron 表达式: " + cron);
        }
    }
}
