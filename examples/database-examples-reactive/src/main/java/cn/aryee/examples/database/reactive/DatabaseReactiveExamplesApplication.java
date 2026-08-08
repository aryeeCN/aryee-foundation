package cn.aryee.examples.database.reactive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Aryee Database Reactive 模式示例应用启动类
 * 用于验证 aryee-foundation-database 模块 Reactive 模式（R2DBC）连接 MySQL 的功能
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
public class DatabaseReactiveExamplesApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatabaseReactiveExamplesApplication.class, args);
    }
}
