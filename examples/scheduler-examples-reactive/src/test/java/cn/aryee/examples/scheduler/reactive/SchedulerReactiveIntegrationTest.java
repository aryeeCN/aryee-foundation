package cn.aryee.examples.scheduler.reactive;

import cn.aryee.scheduler.api.config.SchedulerProperties;
import cn.aryee.scheduler.api.enums.JobType;
import cn.aryee.scheduler.api.model.*;
import cn.aryee.scheduler.api.service.ReactiveJobService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import reactor.test.StepVerifier;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Scheduler Reactive 集成测试
 * 验证响应式多任务类型、批量操作、查询过滤、重试管理、统计监控
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootTest
@DisplayName("Scheduler 响应式功能完整性集成测试")
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
public class SchedulerReactiveIntegrationTest {

    private final SchedulerProperties schedulerProperties;

    private final ReactiveJobService reactiveJobService;

    @Test
    @DisplayName("Reactive 配置加载测试")
    void testSchedulerPropertiesLoaded() {
        assertNotNull(schedulerProperties);
        assertTrue(schedulerProperties.isEnabled());
    }

    @Test
    @DisplayName("Reactive Java 任务添加和查询测试")
    void testAddJavaJob() {
        JobInfo job = new JobInfo();
        job.setJobName("响应式测试任务");
        job.setJobGroup("reactive-test");
        job.setJobType(JobType.JAVA);
        job.setJobClassName("cn.aryee.examples.scheduler.reactive.jobs.SimpleReactiveJob");
        job.setCronExpression("0 0/15 * * * ?");
        job.setPriority(7);

        StepVerifier.create(reactiveJobService.addJob(job))
                .assertNext(jobId -> {
                    assertNotNull(jobId);
                    assertFalse(jobId.isEmpty());
                })
                .verifyComplete();

        StepVerifier.create(reactiveJobService.getJob(job.getJobId()))
                .assertNext(retrieved -> {
                    assertEquals("响应式测试任务", retrieved.getJobName());
                    assertEquals(JobType.JAVA, retrieved.getJobType());
                    assertEquals(7, retrieved.getPriority());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Reactive HTTP 任务测试")
    void testHttpJob() {
        JobInfo job = new JobInfo();
        job.setJobName("响应式HTTP任务");
        job.setJobGroup("reactive-http");
        job.setJobType(JobType.HTTP);
        job.setCronExpression("0 0/5 * * * ?");

        HttpJobConfig httpConfig = HttpJobConfig.get("https://httpbin.org/get");
        job.setHttpConfig(httpConfig);

        StepVerifier.create(reactiveJobService.addJob(job))
                .assertNext(jobId -> assertNotNull(jobId))
                .verifyComplete();

        StepVerifier.create(reactiveJobService.getJob(job.getJobId()))
                .assertNext(retrieved -> {
                    assertEquals(JobType.HTTP, retrieved.getJobType());
                    assertNotNull(retrieved.getHttpConfig());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Reactive Bean 方法任务测试")
    void testBeanMethodJob() {
        JobInfo job = new JobInfo();
        job.setJobName("响应式Bean方法任务");
        job.setJobGroup("reactive-bean");
        job.setJobType(JobType.BEAN_METHOD);
        job.setCronExpression("0 0 * * * ?");

        BeanMethodJobConfig beanConfig = BeanMethodJobConfig.of("testBean", "execute");
        job.setBeanMethodConfig(beanConfig);

        StepVerifier.create(reactiveJobService.addJob(job))
                .assertNext(jobId -> assertNotNull(jobId))
                .verifyComplete();

        StepVerifier.create(reactiveJobService.getJob(job.getJobId()))
                .assertNext(retrieved -> {
                    assertEquals(JobType.BEAN_METHOD, retrieved.getJobType());
                    assertNotNull(retrieved.getBeanMethodConfig());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Reactive 重试策略测试")
    void testRetryPolicy() {
        JobInfo job = new JobInfo();
        job.setJobName("响应式重试任务");
        job.setJobGroup("reactive-retry");
        job.setJobType(JobType.JAVA);
        job.setCronExpression("0 0 * * * ?");
        job.setRetryPolicy(RetryPolicy.exponentialBackoff(5, 2000, 2.0, 60000));

        StepVerifier.create(reactiveJobService.addJob(job))
                .assertNext(jobId -> assertNotNull(jobId))
                .verifyComplete();

        StepVerifier.create(reactiveJobService.getCurrentRetryCount(job.getJobId()))
                .assertNext(count -> assertEquals(0, count))
                .verifyComplete();
    }

    @Test
    @DisplayName("Reactive 批量操作测试")
    void testBatchOperations() {
        List<String> jobIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            JobInfo job = new JobInfo();
            job.setJobName("响应式批量任务-" + i);
            job.setJobGroup("reactive-batch");
            job.setJobType(JobType.JAVA);
            job.setCronExpression("0 0 * * * ?");

            StepVerifier.create(reactiveJobService.addJob(job))
                    .assertNext(jobId -> jobIds.add(jobId))
                    .verifyComplete();
        }

        StepVerifier.create(reactiveJobService.batchPauseJobs(jobIds))
                .assertNext(count -> assertEquals(3, count))
                .verifyComplete();

        StepVerifier.create(reactiveJobService.batchResumeJobs(jobIds))
                .assertNext(count -> assertEquals(3, count))
                .verifyComplete();

        StepVerifier.create(reactiveJobService.batchDeleteJobs(jobIds))
                .assertNext(count -> assertEquals(3, count))
                .verifyComplete();
    }

    @Test
    @DisplayName("Reactive 任务统计测试")
    void testJobStats() {
        JobInfo job = new JobInfo();
        job.setJobName("响应式统计任务");
        job.setJobGroup("reactive-stats");
        job.setJobType(JobType.JAVA);
        job.setCronExpression("0 0 * * * ?");

        StepVerifier.create(reactiveJobService.addJob(job))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(reactiveJobService.getJobStats(job.getJobId()))
                .assertNext(stats -> {
                    assertNotNull(stats);
                    assertEquals(0, stats.getTotalExecutions());
                    assertTrue(stats.isHealthy());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Reactive 调度器全局统计测试")
    void testSchedulerStats() {
        StepVerifier.create(reactiveJobService.getSchedulerStats())
                .assertNext(stats -> {
                    assertNotNull(stats);
                    assertTrue(stats.containsKey("totalJobs"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Reactive 任务查询测试")
    void testJobQueries() {
        JobInfo job = new JobInfo();
        job.setJobName("响应式查询任务");
        job.setJobGroup("reactive-query");
        job.setJobType(JobType.JAVA);
        job.setCronExpression("0 0 * * * ?");

        StepVerifier.create(reactiveJobService.addJob(job))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(reactiveJobService.getJobsByGroup("reactive-query").collectList())
                .assertNext(jobs -> assertFalse(jobs.isEmpty()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Reactive 任务克隆测试")
    void testJobClone() {
        JobInfo source = new JobInfo();
        source.setJobName("响应式源任务-克隆");
        source.setJobGroup("reactive-clone");
        source.setJobType(JobType.JAVA);
        source.setCronExpression("0 0 * * * ?");

        StepVerifier.create(reactiveJobService.addJob(source))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(reactiveJobService.cloneJob(source.getJobId(), "克隆后的任务"))
                .assertNext(clonedId -> {
                    assertNotNull(clonedId);
                    assertFalse(clonedId.isEmpty());
                })
                .verifyComplete();
    }
}
