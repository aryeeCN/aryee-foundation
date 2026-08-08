package cn.aryee.examples.architecture.cloudnative;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 云原生架构形态示例应用启动类
 * <p>
 * 演示 {@code bom-full} + 按需模块 Starter 组合开箱即用。
 * K8s ConfigMap 配置热刷新 + Service Mesh 流量治理 + OpenTelemetry 监控。
 *
 * @author Aryee
 * @since 1.2.0
 */
@SpringBootApplication
public class ArchitectureCloudNativeExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArchitectureCloudNativeExampleApplication.class, args);
    }
}