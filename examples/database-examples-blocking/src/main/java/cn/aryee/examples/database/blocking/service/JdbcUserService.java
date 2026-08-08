package cn.aryee.examples.database.blocking.service;

import cn.aryee.database.infrastructure.blocking.jdbc.service.BaseJdbcDataService;
import cn.aryee.examples.database.blocking.entity.JdbcUser;
import cn.aryee.examples.database.blocking.repository.JdbcUserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * JDBC 模式用户服务
 * 继承 BaseJdbcDataService 自动获得 BaseDataService 全部能力
 * 仅在激活 "jdbc" profile 时创建
 *
 * @author Aryee
 * @since 1.0.0
 */
@Service("jdbcUserService")
@Profile("jdbc")
public class JdbcUserService extends BaseJdbcDataService<JdbcUser, Long> {

    public JdbcUserService(JdbcUserRepository repository) {
        super(repository);
    }
}
