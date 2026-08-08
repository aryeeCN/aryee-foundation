package cn.aryee.examples.database.oracle.blocking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aryee Database Blocking 模式 Oracle 示例应用启动类。
 * 使用 JDBC (MyBatis-Plus) 模式连接 Oracle 23ai Free 数据库。
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
public class DatabaseOracleBlockingExamplesApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatabaseOracleBlockingExamplesApplication.class, args);
    }
}
