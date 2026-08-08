package cn.aryee.examples.architecture.microservice.repository;

import cn.aryee.examples.architecture.microservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 订单仓库（微服务架构示例）
 *
 * @author Aryee
 * @since 1.2.0
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
}