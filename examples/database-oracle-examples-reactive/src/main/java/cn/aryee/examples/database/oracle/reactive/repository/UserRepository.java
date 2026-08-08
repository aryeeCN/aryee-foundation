package cn.aryee.examples.database.oracle.reactive.repository;

import cn.aryee.database.infrastructure.reactive.r2dbc.repository.BaseR2dbcRepository;
import cn.aryee.examples.database.oracle.reactive.entity.User;
import org.springframework.stereotype.Repository;

/**
 * 用户 Repository（R2DBC - Oracle）。
 * 继承 {@link BaseR2dbcRepository} 自动获得标准 CRUD 能力。
 *
 * @author Aryee
 * @since 1.0.0
 */
@Repository
public interface UserRepository extends BaseR2dbcRepository<User, Long> {
}
