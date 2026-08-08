package cn.aryee.examples.architecture.monolith.repository;

import cn.aryee.examples.architecture.monolith.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 用户仓库（单体架构示例）
 *
 * @author Aryee
 * @since 1.2.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}