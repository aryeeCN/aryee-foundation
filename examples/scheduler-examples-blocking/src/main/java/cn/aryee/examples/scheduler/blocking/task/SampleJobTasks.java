package cn.aryee.examples.scheduler.blocking.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 示例定时任务 Bean
 * 演示 BEAN_METHOD 类型任务调用的目标方法
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@Component("sampleJobTasks")
public class SampleJobTasks {

    /**
     * 无参任务：简单的数据同步
     */
    public void syncData() {
        log.info("[定时任务] 数据同步开始: {}", LocalDateTime.now());
        // 模拟业务逻辑
        sleep(500);
        log.info("[定时任务] 数据同步完成");
    }

    /**
     * 带参任务：处理指定 ID 的订单
     */
    public void processOrder(String orderId, int retryCount) {
        log.info("[定时任务] 处理订单: orderId={}, retryCount={}, time={}", orderId, retryCount, LocalDateTime.now());
        sleep(300);
        log.info("[定时任务] 订单处理完成: {}", orderId);
    }

    /**
     * 带返回值的任务
     */
    public String generateReport(String reportType) {
        log.info("[定时任务] 生成报表: type={}", reportType);
        sleep(800);
        String result = "report-" + reportType + "-" + System.currentTimeMillis();
        log.info("[定时任务] 报表生成完成: {}", result);
        return result;
    }

    /**
     * 模拟失败任务（用于演示重试机制）
     */
    public void unstableTask() {
        double random = Math.random();
        log.info("[定时任务] 执行不稳定任务, random={}", random);
        if (random < 0.5) {
            throw new RuntimeException("模拟任务执行失败");
        }
        log.info("[定时任务] 不稳定任务执行成功");
    }

    /**
     * 长时间运行任务（用于演示超时控制）
     */
    public void longRunningTask() {
        log.info("[定时任务] 长时间任务开始");
        for (int i = 1; i <= 10; i++) {
            log.info("[定时任务] 进度: {}/10", i);
            sleep(1000);
        }
        log.info("[定时任务] 长时间任务完成");
    }

    private void sleep(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
