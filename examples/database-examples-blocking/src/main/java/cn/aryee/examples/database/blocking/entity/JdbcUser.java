package cn.aryee.examples.database.blocking.entity;

import cn.aryee.database.infrastructure.blocking.jdbc.model.BaseJdbcEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 示例用户实体（JDBC/MyBatis-Plus 模式）。
 * 继承 {@link BaseJdbcEntity} 自动获得 id/createTime/updateTime/version/deleted 字段。
 * 仅包含 MyBatis-Plus 注解，不包含 JPA 注解。
 *
 * @author Aryee
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("aryee_test_user")
public class JdbcUser extends BaseJdbcEntity {

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 状态（1启用 0禁用）
     */
    private Integer status;
}
