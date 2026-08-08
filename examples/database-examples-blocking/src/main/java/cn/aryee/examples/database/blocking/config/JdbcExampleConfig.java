package cn.aryee.examples.database.blocking.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * JDBC 模式示例配置
 * 仅在激活 "jdbc" profile 时扫描 Mapper 接口
 *
 * @author Aryee
 * @since 1.0.0
 */
@Configuration
@Profile("jdbc")
@MapperScan(basePackages = {
        "cn.aryee.examples.database.blocking.mapper",
        "cn.aryee.database.infrastructure.blocking.jdbc.repository"
})
public class JdbcExampleConfig {
}
