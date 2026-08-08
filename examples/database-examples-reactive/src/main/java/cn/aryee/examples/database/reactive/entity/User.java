package cn.aryee.examples.database.reactive.entity;

import cn.aryee.database.infrastructure.reactive.r2dbc.model.BaseR2dbcEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 示例用户实体（R2DBC）
 * 继承 BaseR2dbcEntity 自动获得 id/createTime/updateTime/version/deleted 字段
 * 使用 Spring Data R2DBC 注解（@Table）
 *
 * @author Aryee
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("aryee_test_user_r2dbc")
public class User extends BaseR2dbcEntity {

    /**
     * 用户名
     */
    @Column("username")
    private String username;

    /**
     * 邮箱
     */
    @Column("email")
    private String email;

    /**
     * 年龄
     */
    @Column("age")
    private Integer age;

    /**
     * 状态（1启用 0禁用）
     */
    @Column("status")
    private Integer status;
}
