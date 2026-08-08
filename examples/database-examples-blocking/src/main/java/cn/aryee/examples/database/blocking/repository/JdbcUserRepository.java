package cn.aryee.examples.database.blocking.repository;

import cn.aryee.database.infrastructure.blocking.jdbc.repository.BaseJdbcRepository;
import cn.aryee.examples.database.blocking.entity.JdbcUser;
import cn.aryee.examples.database.blocking.mapper.UserMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * JDBC 模式用户仓储
 * 继承 BaseJdbcRepository 获得 MyBatis-Plus 基础仓储能力
 * 仅在激活 "jdbc" profile 时创建
 *
 * @author Aryee
 * @since 1.0.0
 */
@Repository("jdbcUserRepository")
@Profile("jdbc")
public class JdbcUserRepository extends BaseJdbcRepository<JdbcUser, UserMapper> {

    public JdbcUserRepository(UserMapper mapper) {
        super(mapper);
    }
}
