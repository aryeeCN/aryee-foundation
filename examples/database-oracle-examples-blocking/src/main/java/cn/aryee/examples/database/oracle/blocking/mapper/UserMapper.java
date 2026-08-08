package cn.aryee.examples.database.oracle.blocking.mapper;

import cn.aryee.examples.database.oracle.blocking.entity.JdbcUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * JDBC 模式用户 Mapper 接口
 * 继承 BaseMapper 获得 MyBatis-Plus 基础 CRUD 能力
 *
 * @author Aryee
 * @since 1.0.0
 */
public interface UserMapper extends BaseMapper<JdbcUser> {
}
