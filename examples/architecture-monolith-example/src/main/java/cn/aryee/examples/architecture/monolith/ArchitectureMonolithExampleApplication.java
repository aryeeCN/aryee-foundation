package cn.aryee.examples.architecture.monolith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 单体架构形态示例应用启动类
 * <p>
 * 演示 {@code bom-full} + 按需模块 Starter 组合开箱即用。
 * 零外部依赖，H2 嵌入式数据库 + Caffeine 本地缓存 + 本地文件存储。
 *
 * @author Aryee
 * @since 1.2.0
 */
@SpringBootApplication
public class ArchitectureMonolithExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArchitectureMonolithExampleApplication.class, args);
    }
}