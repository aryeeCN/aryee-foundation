package cn.aryee.examples.database.blocking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aryee Database Blocking 模式示例应用启动类。
 * 同时支持 JPA 和 JDBC (MyBatis-Plus) 两种模式，通过 Spring Profile 切换：
 * <ul>
 *   <li>{@code test} profile：JPA 模式（由 {@link cn.aryee.examples.database.blocking.config.JpaProfileConfig} 启用 JPA Repository 扫描）</li>
 *   <li>{@code jdbc} profile：JDBC (MyBatis-Plus) 模式（由 {@link cn.aryee.examples.database.blocking.config.JdbcExampleConfig} 启用 Mapper 扫描）</li>
 * </ul>
 *
 * <p>依赖说明：
 * <ul>
 *   <li>仅引入 database-spring-boot-starter（Blocking Starter）</li>
 *   <li>禁止同时引入 database-reactive-spring-boot-starter（架构规则 6.1 Starter 隔离）</li>
 * </ul>
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootApplication
public class DatabaseBlockingExamplesApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatabaseBlockingExamplesApplication.class, args);
    }
}
