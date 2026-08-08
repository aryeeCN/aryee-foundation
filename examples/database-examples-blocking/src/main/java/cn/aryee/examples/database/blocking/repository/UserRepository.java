package cn.aryee.examples.database.blocking.repository;

import cn.aryee.database.infrastructure.blocking.jpa.repository.BaseRepository;
import cn.aryee.examples.database.blocking.entity.User;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * 用户仓储接口（JPA 模式）
 * 继承 BaseRepository 自动获得 JpaRepository + JpaSpecificationExecutor 能力
 * 仅在激活 "test" profile 时创建
 *
 * @author Aryee
 * @since 1.0.0
 */
@Repository
@Profile("test")
public interface UserRepository extends BaseRepository<User, Long> {
}
