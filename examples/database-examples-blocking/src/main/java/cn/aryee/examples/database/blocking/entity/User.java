package cn.aryee.examples.database.blocking.entity;

import cn.aryee.database.infrastructure.blocking.jpa.model.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 示例用户实体（JPA 模式）。
 * 继承 {@link BaseJpaEntity} 自动获得 id/createTime/updateTime/version/deleted 字段。
 * 仅包含 JPA 注解，不包含 MyBatis-Plus 注解。
 *
 * @author Aryee
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "aryee_test_user")
public class User extends BaseJpaEntity {

    /**
     * 用户名
     */
    @Column(name = "username", length = 64, nullable = false)
    private String username;

    /**
     * 邮箱
     */
    @Column(name = "email", length = 128)
    private String email;

    /**
     * 年龄
     */
    @Column(name = "age")
    private Integer age;

    /**
     * 状态（1启用 0禁用）
     */
    @Column(name = "status")
    private Integer status;
}
