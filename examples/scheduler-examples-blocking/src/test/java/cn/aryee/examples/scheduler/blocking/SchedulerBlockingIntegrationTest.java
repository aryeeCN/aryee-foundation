package cn.aryee.examples.scheduler.blocking;

import cn.aryee.scheduler.api.config.SchedulerProperties;
import cn.aryee.scheduler.api.enums.JobType;
import cn.aryee.scheduler.api.model.*;
import cn.aryee.scheduler.api.service.JobService;
import cn.aryee.scheduler.api.service.SchedulerManagementService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Scheduler Blocking 集成测试
 * 验证多任务类型、批量操作、查询过滤、重试管理、统计监控、生命周期管理
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootTest
@DisplayName("Scheduler 功能完整性集成测试")
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
public class SchedulerBlockingIntegrationTest {

    private final SchedulerProperties schedulerProperties;

    private final JobService jobService;

    private final SchedulerManagementService schedulerManagementService;

    @Test
    @DisplayName("配置加载测试")
    void testSchedulerPropertiesLoaded() {
        assertNotNull(schedulerProperties);
        assertTrue(schedulerProperties.isEnabled());
        assertEquals("quartz", schedulerProperties.getType());

        SchedulerProperties.ThreadPool threadPool = schedulerProperties.getThreadPool();
        assertEquals(10, threadPool.getPoolSize());

        SchedulerProperties.Retry retry = schedulerProperties.getRetry();
        assertEquals(3, retry.getDefaultMaxRetries());

        SchedulerProperties.Monitoring monitoring = schedulerProperties.getMonitoring();
        assertTrue(monitoring.isEnabled());
    }

    @Test
    @DisplayName("调度器生命周期管理测试")
    void testSchedulerLifecycle() {
        assertNotNull(schedulerManagementService);
        assertNotNull(schedulerManagementService.getSchedulerInstanceId());
        assertNotNull(schedulerManagementService.getMetadata());
    }

    @Test
    @DisplayName("健康检查测试")
    void testHealthCheck() {
        Map<String, Object> health = schedulerManagementService.getHealth();
        assertNotNull(health);
        assertNotNull(health.get("status"));
    }

    @Test
    @DisplayName("Java 类任务添加和查询测试")
    void testAddJavaJob() {
        JobInfo job = new JobInfo();
        job.setJobName("测试-数据同步");
        job.setJobGroup("data-sync");
        job.setJobType(JobType.JAVA);
        job.setJobClassName("cn.aryee.examples.scheduler.blocking.jobs.DataSyncJob");
        job.setCronExpression("0 0/30 * * * ?");
        job.setDescription("每30分钟数据同步");
        job.setPriority(8);
        job.setCategory("data");
        job.setTags(new HashSet<>(Arrays.asList("sync", "important")));

        String jobId = jobService.addJob(job);
        assertNotNull(jobId);

        JobInfo retrieved = jobService.getJob(jobId);
        assertNotNull(retrieved);
        assertEquals("测试-数据同步", retrieved.getJobName());
        assertEquals(JobType.JAVA, retrieved.getJobType());
        assertEquals(8, retrieved.getPriority());
    }

    @Test
    @DisplayName("Bean 方法任务测试")
    void testBeanMethodJob() {
        JobInfo job = new JobInfo();
        job.setJobName("Bean方法任务测试");
        job.setJobGroup("test");
        job.setJobType(JobType.BEAN_METHOD);
        job.setCronExpression("0 0 * * * ?");

        BeanMethodJobConfig beanConfig = BeanMethodJobConfig.of("testJobService", "execute");
        job.setBeanMethodConfig(beanConfig);

        String jobId = jobService.addJob(job);
        assertNotNull(jobId);

        JobInfo retrieved = jobService.getJob(jobId);
        assertEquals(JobType.BEAN_METHOD, retrieved.getJobType());
        assertNotNull(retrieved.getBeanMethodConfig());
    }

    @Test
    @DisplayName("HTTP 调用任务测试")
    void testHttpJob() {
        JobInfo job = new JobInfo();
        job.setJobName("HTTP健康检查任务");
        job.setJobGroup("monitoring");
        job.setJobType(JobType.HTTP);
        job.setCronExpression("0 0/5 * * * ?");

        HttpJobConfig httpConfig = HttpJobConfig.get("https://httpbin.org/get");
        httpConfig.addHeader("Accept", "application/json");
        httpConfig.setConnectTimeoutMs(5000);
        job.setHttpConfig(httpConfig);

        String jobId = jobService.addJob(job);
        assertNotNull(jobId);

        JobInfo retrieved = jobService.getJob(jobId);
        assertEquals(JobType.HTTP, retrieved.getJobType());
        assertEquals("GET", retrieved.getHttpConfig().getMethod());
    }

    @Test
    @DisplayName("重试策略测试")
    void testRetryPolicy() {
        JobInfo job = new JobInfo();
        job.setJobName("重试测试任务");
        job.setJobGroup("test");
        job.setJobType(JobType.JAVA);
        job.setJobClassName("cn.aryee.examples.scheduler.blocking.jobs.RetryTestJob");
        job.setCronExpression("0 0 * * * ?");

        RetryPolicy retryPolicy = RetryPolicy.exponentialBackoff(5, 1000, 2.0, 30000);
        job.setRetryPolicy(retryPolicy);

        String jobId = jobService.addJob(job);

        assertEquals(0, jobService.getCurrentRetryCount(jobId));

        jobService.resetRetryCount(jobId);
        assertEquals(0, jobService.getCurrentRetryCount(jobId));
    }

    @Test
    @DisplayName("批量操作测试")
    void testBatchOperations() {
        List<String> jobIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            JobInfo job = new JobInfo();
            job.setJobName("批量任务-" + i);
            job.setJobGroup("batch-test");
            job.setJobType(JobType.JAVA);
            job.setJobClassName("cn.aryee.examples.scheduler.blocking.jobs.SimpleJob");
            job.setCronExpression("0 0 * * * ?");
            jobIds.add(jobService.addJob(job));
        }

        int paused = jobService.batchPauseJobs(jobIds);
        assertEquals(5, paused);

        int resumed = jobService.batchResumeJobs(jobIds);
        assertEquals(5, resumed);

        int deleted = jobService.batchDeleteJobs(jobIds);
        assertEquals(5, deleted);
    }

    @Test
    @DisplayName("任务查询测试")
    void testJobQueries() {
        JobInfo job1 = new JobInfo();
        job1.setJobName("报表任务-A");
        job1.setJobGroup("report");
        job1.setJobType(JobType.JAVA);
        job1.setCronExpression("0 0 * * * ?");
        jobService.addJob(job1);

        JobInfo job2 = new JobInfo();
        job2.setJobName("报表任务-B");
        job2.setJobGroup("report");
        job2.setJobType(JobType.JAVA);
        job2.setCronExpression("0 0 * * * ?");
        jobService.addJob(job2);

        List<JobInfo> groupJobs = jobService.getJobsByGroup("report");
        assertFalse(groupJobs.isEmpty());

        List<JobInfo> searchResults = jobService.searchJobs("报表");
        assertFalse(searchResults.isEmpty());
    }

    @Test
    @DisplayName("任务克隆测试")
    void testJobClone() {
        JobInfo job = new JobInfo();
        job.setJobName("原始任务-克隆测试");
        job.setJobGroup("clone-test");
        job.setJobType(JobType.JAVA);
        job.setJobClassName("cn.aryee.examples.scheduler.blocking.jobs.SimpleJob");
        job.setCronExpression("0 0 * * * ?");
        String sourceId = jobService.addJob(job);

        String clonedId = jobService.cloneJob(sourceId, "克隆任务");
        assertNotNull(clonedId);

        JobInfo cloned = jobService.getJob(clonedId);
        assertEquals("克隆任务", cloned.getJobName());
        assertEquals("clone-test", cloned.getJobGroup());
    }

    @Test
    @DisplayName("任务统计测试")
    void testJobStats() {
        JobInfo job = new JobInfo();
        job.setJobName("统计测试任务");
        job.setJobGroup("stats-test");
        job.setJobType(JobType.JAVA);
        job.setCronExpression("0 0 * * * ?");
        String jobId = jobService.addJob(job);

        JobExecutionStats stats = jobService.getJobStats(jobId);
        assertNotNull(stats);
        assertEquals(0, stats.getTotalExecutions());
        assertTrue(stats.isHealthy());
    }

    @Test
    @DisplayName("调度器全局统计测试")
    void testSchedulerStats() {
        Map<String, Object> stats = jobService.getSchedulerStats();
        assertNotNull(stats);
        assertTrue(stats.containsKey("totalJobs"));
        assertTrue(stats.containsKey("runningJobs"));
    }

    @Test
    @DisplayName("监听器管理测试")
    void testListenerManagement() {
        assertNotNull(schedulerManagementService.getListeners());
    }
}
