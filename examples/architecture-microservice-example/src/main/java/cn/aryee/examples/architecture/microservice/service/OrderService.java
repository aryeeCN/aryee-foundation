package cn.aryee.examples.architecture.microservice.service;

import cn.aryee.examples.architecture.microservice.entity.Order;
import cn.aryee.examples.architecture.microservice.repository.OrderRepository;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 订单服务（微服务架构示例）
 * <p>
 * 演示 Seata 分布式事务 + Redis 缓存 + 本地事务的组合使用。
 *
 * @author Aryee
 * @since 1.2.0
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * 创建订单（本地事务）
     */
    @Transactional
    public Order createOrder(Long userId, Long productId, String productName, int quantity, BigDecimal price) {
        Order order = Order.builder()
                .orderNo(UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase())
                .userId(userId)
                .productId(productId)
                .productName(productName)
                .quantity(quantity)
                .totalAmount(price.multiply(BigDecimal.valueOf(quantity)))
                .status("CONFIRMED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Order saved = orderRepository.save(order);
        log.info("订单创建成功: orderNo={}, amount={}", saved.getOrderNo(), saved.getTotalAmount());
        return saved;
    }

    /**
     * 创建订单（Distributed Transaction）
     * <p>
     * 使用 {@code @GlobalTransactional} 开启 Seata 分布式事务，
     * 当 Seata 不在类路径上时，退化为 Spring 本地事务。
     */
    @GlobalTransactional(name = "create-order-tx", rollbackFor = Exception.class)
    public Order createOrderDistributed(Long userId, Long productId, String productName,
                                        int quantity, BigDecimal price) {
        log.info("分布式事务开始: xid={}", RootContext.getXID());

        // 步骤1：创建订单
        Order order = createOrder(userId, productId, productName, quantity, price);

        // 步骤2：扣减库存（远程调用，由 Seata 保证一致性）
        // 实际场景中通过 OpenFeign 调用库存服务
        // storageClient.deduct(productId, quantity);

        // 步骤3：扣减余额（远程调用，由 Seata 保证一致性）
        // accountClient.debit(userId, order.getTotalAmount());

        log.info("分布式事务完成: orderNo={}, xid={}", order.getOrderNo(), RootContext.getXID());
        return order;
    }

    /**
     * 获取订单（Redis 缓存加速）
     */
    @Cacheable(value = "orders", key = "#id")
    public Optional<Order> getOrder(Long id) {
        log.info("从数据库加载订单: id={}", id);
        return orderRepository.findById(id);
    }

    /**
     * 获取所有订单
     */
    public List<Order> listOrders() {
        return orderRepository.findAll();
    }

    /**
     * 取消订单（缓存自动失效）
     */
    @Transactional
    @CacheEvict(value = "orders", key = "#id")
    public Optional<Order> cancelOrder(Long id) {
        return orderRepository.findById(id)
                .map(order -> {
                    order.setStatus("CANCELLED");
                    order.setUpdatedAt(LocalDateTime.now());
                    log.info("订单已取消: orderNo={}", order.getOrderNo());
                    return orderRepository.save(order);
                });
    }
}