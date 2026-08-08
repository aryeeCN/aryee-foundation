package cn.aryee.examples.architecture.microservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体（微服务架构示例）
 * <p>
 * 演示 Seata 分布式事务下的订单创建流程。
 *
 * @author Aryee
 * @since 1.2.0
 */
@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 订单编号 */
    private String orderNo;

    /** 用户 ID */
    private Long userId;

    /** 商品 ID */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 数量 */
    private Integer quantity;

    /** 总金额 */
    private BigDecimal totalAmount;

    /** 订单状态：PENDING/CONFIRMED/CANCELLED */
    private String status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}