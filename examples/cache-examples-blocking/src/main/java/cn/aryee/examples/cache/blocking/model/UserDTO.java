package cn.aryee.examples.cache.blocking.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户数据传输对象，用于缓存示例。
 * <p>
 * 作为缓存值时需要实现 {@link Serializable}，
 * 以便 Redis 序列化器（GenericJackson2JsonRedisSerializer）正确处理。
 * </p>
 *
 * @author Aryee
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 邮箱 */
    private String email;

    /** 状态：1-启用，0-禁用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;
}
