package cn.aryee.examples.database.oracle.reactive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aryee Database Reactive 模式 Oracle 示例应用启动类
 * 用于验证 aryee-foundation-database 模块 Reactive 模式（R2DBC）连接 Oracle 23ai 的功能
 *
 * <p>依赖说明：
 * <ul>
 *   <li>仅引入 database-reactive-spring-boot-starter（Reactive Starter）</li>
 *   <li>禁止同时引入 database-spring-boot-starter（架构规则 6.1 Starter 隔离）</li>
 * </ul>
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootApplication
public class DatabaseOracleReactiveExamplesApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatabaseOracleReactiveExamplesApplication.class, args);
    }
}
