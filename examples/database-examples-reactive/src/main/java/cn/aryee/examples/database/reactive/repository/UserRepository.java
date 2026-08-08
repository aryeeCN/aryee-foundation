package cn.aryee.examples.database.reactive.repository;

import cn.aryee.database.infrastructure.reactive.r2dbc.repository.BaseR2dbcRepository;
import cn.aryee.examples.database.reactive.entity.User;
import org.springframework.stereotype.Repository;

/**
 * 用户 Repository（R2DBC）。
 * <p>继承 {@link BaseR2dbcRepository} 自动获得标准 CRUD 能力，Spring Data 自动生成代理实现，无需手动注册 Bean。</p>
 *
 * @author Aryee
 * @since 1.0.0
 */
@Repository
public interface UserRepository extends BaseR2dbcRepository<User, Long> {
}
