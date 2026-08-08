package cn.aryee.examples.architecture.microservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 微服务架构形态示例应用启动类
 * <p>
 * 演示 {@code bom-full} + 按需模块 Starter 组合开箱即用。
 * Nacos 注册中心 + Nacos 配置中心 + Seata 分布式事务 + XXL-Job 分布式调度。
 *
 * @author Aryee
 * @since 1.2.0
 */
@SpringBootApplication
public class ArchitectureMicroserviceExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArchitectureMicroserviceExampleApplication.class, args);
    }
}