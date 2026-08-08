package cn.aryee.examples.database.blocking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA Profile 配置。
 * 仅在激活 "test" profile 时启用 JPA Repository 扫描。
 * 用于 JPA 模式测试。
 *
 * @author Aryee
 * @since 1.0.0
 */
@Configuration
@Profile("test")
@EnableJpaRepositories(basePackages = "cn.aryee.examples.database.blocking.repository")
public class JpaProfileConfig {
}
