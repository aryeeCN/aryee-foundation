package cn.aryee.examples.gateway.reactive.controller;

import cn.aryee.examples.gateway.reactive.service.GatewayReactiveDemoService;
import cn.aryee.gateway.api.aggregate.AggregateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Gateway Reactive 示例 Controller
 * 演示响应式网关功能的 REST API 端点
 * <p>
 * 路由管理由 {@link DynamicRouteController} 提供（基于 foundation-gateway 模块的 DynamicRouteService），
 * 本 Controller 仅演示限流、熔断、灰度发布、API 聚合能力。
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/gateway")
@RequiredArgsConstructor
public class GatewayReactiveDemoController {

    private final GatewayReactiveDemoService gatewayReactiveDemoService;

    // ==================== 限流 ====================

    /**
     * 尝试获取限流许可
     */
    @PostMapping("/rate-limit/try-acquire")
    public Mono<Map<String, Object>> tryAcquire(@RequestParam String key) {
        return gatewayReactiveDemoService.tryAcquire(key);
    }

    /**
     * 查询剩余许可数
     */
    @GetMapping("/rate-limit/remaining")
    public Mono<Map<String, Object>> getRemaining(@RequestParam String key) {
        return gatewayReactiveDemoService.getRemaining(key);
    }

    // ==================== 熔断 ====================

    /**
     * 获取熔断器状态
     */
    @GetMapping("/circuit-breaker/state")
    public Mono<Map<String, Object>> getCircuitBreakerState() {
        return gatewayReactiveDemoService.getCircuitBreakerState();
    }

    /**
     * 重置熔断器
     */
    @PostMapping("/circuit-breaker/reset")
    public Mono<Map<String, Object>> resetCircuitBreaker() {
        return gatewayReactiveDemoService.resetCircuitBreaker();
    }

    /**
     * 执行受熔断保护的操作
     */
    @PostMapping("/circuit-breaker/execute")
    public Mono<Map<String, Object>> executeWithCircuitBreaker(@RequestParam(defaultValue = "false") boolean shouldFail) {
        log.info("执行熔断保护操作: shouldFail={}", shouldFail);
        return gatewayReactiveDemoService.executeWithCircuitBreaker(shouldFail);
    }

    // ==================== 灰度发布 ====================

    /**
     * 灰度路由
     */
    @GetMapping("/canary/route")
    public Mono<Map<String, Object>> canaryRoute(@RequestParam(required = false) String userId,
                                                 @RequestParam(defaultValue = "/api/test") String path,
                                                 @RequestParam(defaultValue = "127.0.0.1") String clientIp) {
        return gatewayReactiveDemoService.canaryRoute(userId, path, clientIp);
    }

    /**
     * 获取灰度配置
     */
    @GetMapping("/canary/config")
    public Mono<Map<String, Object>> getCanaryConfig() {
        return gatewayReactiveDemoService.getCanaryConfig();
    }

    /**
     * 更新灰度权重
     */
    @PutMapping("/canary/weight")
    public Mono<Map<String, Object>> updateCanaryWeight(@RequestParam int weight) {
        log.info("更新灰度权重: weight={}", weight);
        return gatewayReactiveDemoService.updateCanaryWeight(weight);
    }

    /**
     * 灰度用户管理
     */
    @PostMapping("/canary/users")
    public Mono<Map<String, Object>> manageCanaryUser(@RequestParam String userId,
                                                      @RequestParam(defaultValue = "true") boolean add) {
        log.info("灰度用户管理: userId={}, add={}", userId, add);
        return gatewayReactiveDemoService.manageCanaryUser(userId, add);
    }

    // ==================== API 聚合 ====================

    /**
     * 执行 API 聚合
     */
    @PostMapping("/aggregate")
    public Mono<Map<String, Object>> aggregate(@RequestBody List<AggregateRequest> requests) {
        log.info("API 聚合: {} 个请求", requests.size());
        return gatewayReactiveDemoService.aggregateApis(requests);
    }
}
