package cn.aryee.examples.architecture.microservice.controller;

import cn.aryee.examples.architecture.microservice.entity.Order;
import cn.aryee.examples.architecture.microservice.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 订单 REST API（微服务架构示例）
 * <p>
 * 演示分布式事务 + 缓存 + 服务发现的组合使用。
 *
 * @author Aryee
 * @since 1.2.0
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 创建订单（本地事务）
     */
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestParam Long userId,
                                              @RequestParam Long productId,
                                              @RequestParam String productName,
                                              @RequestParam int quantity,
                                              @RequestParam BigDecimal price) {
        Order order = orderService.createOrder(userId, productId, productName, quantity, price);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * 创建订单（分布式事务）
     * <p>
     * 当 Seata 可用时，此接口使用 {@code @GlobalTransactional} 保证跨服务一致性。
     */
    @PostMapping("/distributed")
    public ResponseEntity<Order> createOrderDistributed(@RequestParam Long userId,
                                                         @RequestParam Long productId,
                                                         @RequestParam String productName,
                                                         @RequestParam int quantity,
                                                         @RequestParam BigDecimal price) {
        Order order = orderService.createOrderDistributed(userId, productId, productName, quantity, price);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * 获取订单（Redis 缓存加速）
     */
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        return orderService.getOrder(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取所有订单
     */
    @GetMapping
    public List<Order> listOrders() {
        return orderService.listOrders();
    }

    /**
     * 取消订单
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Order> cancelOrder(@PathVariable Long id) {
        return orderService.cancelOrder(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 服务健康检查
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "order-service");
    }
}