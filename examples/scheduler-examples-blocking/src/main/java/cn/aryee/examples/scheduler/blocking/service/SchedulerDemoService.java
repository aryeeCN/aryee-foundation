package cn.aryee.examples.scheduler.blocking.service;

import cn.aryee.scheduler.api.enums.JobType;
import cn.aryee.scheduler.api.model.*;
import cn.aryee.scheduler.api.service.JobService;
import cn.aryee.scheduler.api.util.CronUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Scheduler 演示服务
 * 封装各种任务类型的创建逻辑
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerDemoService {

    private final JobService jobService;

    /**
     * 创建 BEAN_METHOD 类型任务（禁止并发）
     */
    public String createBeanMethodJob(String jobName, String cron, String beanName, String methodName) {
        validateCron(cron);

        JobInfo jobInfo = new JobInfo();
        jobInfo.setJobName(jobName);
        jobInfo.setJobGroup("demo");
        jobInfo.setJobType(JobType.BEAN_METHOD);
        jobInfo.setCronExpression(cron);
        jobInfo.setConcurrent(false); // 禁止并发
        jobInfo.setDurable(true);
        jobInfo.setRequestsRecovery(true);

        BeanMethodJobConfig beanConfig = BeanMethodJobConfig.of(beanName, methodName);
        jobInfo.setBeanMethodConfig(beanConfig);

        return jobService.addJob(jobInfo);
    }

    /**
     * 创建带参数的 BEAN_METHOD 任务
     */
    public String createBeanMethodWithArgs(String jobName, String cron,
                                            String beanName, String methodName, Object[] args) {
        validateCron(cron);

        JobInfo jobInfo = new JobInfo();
        jobInfo.setJobName(jobName);
        jobInfo.setJobGroup("demo");
        jobInfo.setJobType(JobType.BEAN_METHOD);
        jobInfo.setCronExpression(cron);
        jobInfo.setConcurrent(true); // 允许并发

        BeanMethodJobConfig beanConfig = BeanMethodJobConfig.of(beanName, methodName, args);
        jobInfo.setBeanMethodConfig(beanConfig);

        return jobService.addJob(jobInfo);
    }

    /**
     * 创建 HTTP 调用任务
     */
    public String createHttpJob(String jobName, String cron, String url, String method) {
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

        return jobService.addJob(jobInfo);
    }

    /**
     * 创建带重试策略的任务
     */
    public String createJobWithRetry(String jobName, String cron, String beanName, String methodName,
                                      int maxRetries, long retryIntervalMs) {
        validateCron(cron);

        JobInfo jobInfo = new JobInfo();
        jobInfo.setJobName(jobName);
        jobInfo.setJobGroup("demo-retry");
        jobInfo.setJobType(JobType.BEAN_METHOD);
        jobInfo.setCronExpression(cron);
        jobInfo.setConcurrent(false);

        BeanMethodJobConfig beanConfig = BeanMethodJobConfig.of(beanName, methodName);
        jobInfo.setBeanMethodConfig(beanConfig);

        // 设置重试策略：指数退避
        RetryPolicy retryPolicy = RetryPolicy.exponentialBackoff(maxRetries, retryIntervalMs, 2.0, 60000);
        jobInfo.setRetryPolicy(retryPolicy);

        return jobService.addJob(jobInfo);
    }

    /**
     * 创建带超时的任务
     */
    public String createJobWithTimeout(String jobName, String cron, String beanName, String methodName,
                                        long timeoutMs) {
        validateCron(cron);

        JobInfo jobInfo = new JobInfo();
        jobInfo.setJobName(jobName);
        jobInfo.setJobGroup("demo-timeout");
        jobInfo.setJobType(JobType.BEAN_METHOD);
        jobInfo.setCronExpression(cron);
        jobInfo.setConcurrent(false);
        jobInfo.setTimeoutMs(timeoutMs);

        BeanMethodJobConfig beanConfig = BeanMethodJobConfig.of(beanName, methodName);
        jobInfo.setBeanMethodConfig(beanConfig);

        return jobService.addJob(jobInfo);
    }

    /**
     * 获取所有任务
     */
    public List<JobInfo> listJobs() {
        return jobService.getAllJobs();
    }

    /**
     * 获取任务执行日志
     */
    public List<JobExecutionLog> getJobLogs(String jobId) {
        return jobService.getJobExecutionLogs(jobId, 10, 1);
    }

    /**
     * 获取调度器统计
     */
    public java.util.Map<String, Object> getSchedulerStats() {
        return jobService.getSchedulerStats();
    }

    /**
     * 验证 Cron 表达式
     */
    private void validateCron(String cron) {
        if (!CronUtil.isValid(cron)) {
            throw new IllegalArgumentException("无效的 Cron 表达式: " + cron);
        }
    }
}
