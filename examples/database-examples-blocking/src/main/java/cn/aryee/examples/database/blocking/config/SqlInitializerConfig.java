package cn.aryee.examples.database.blocking.config;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import lombok.RequiredArgsConstructor;

/**
 * SQL 初始化配置类。
 * <p>
 * 在应用启动时执行 schema-jdbc.sql 脚本，确保表结构正确创建。
 * 仅在 JDBC 模式（jdbc profile）下激活。
 * </p>
 *
 * @author Aryee
 * @since 1.0.0
 */
@Configuration
@Profile("jdbc")
@RequiredArgsConstructor
public class SqlInitializerConfig implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SqlInitializerConfig.class);

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        log.info("开始初始化 JDBC 模式示例数据库表结构...");

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setSqlScriptEncoding("UTF-8");
        populator.setContinueOnError(true);
        populator.addScript(new ClassPathResource("schema-jdbc.sql"));

        populator.execute(dataSource);

        log.info("JDBC 模式示例数据库表结构初始化完成。");
    }
}
