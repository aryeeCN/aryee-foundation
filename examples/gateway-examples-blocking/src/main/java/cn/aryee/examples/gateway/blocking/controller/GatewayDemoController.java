package cn.aryee.examples.gateway.blocking.controller;

import cn.aryee.examples.gateway.blocking.service.GatewayDemoService;
import cn.aryee.gateway.api.aggregate.AggregateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Gateway Blocking 示例 Controller
 * 演示限流、熔断、灰度发布、API 聚合等网关功能的 REST API 端点
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/gateway")
@RequiredArgsConstructor
public class GatewayDemoController {

    private final GatewayDemoService gatewayDemoService;

    // ==================== 路由管理 ====================

    /**
     * 添加路由
     */
    @PostMapping("/routes")
    public Map<String, Object> addRoute(@RequestParam String routeId,
                                         @RequestParam String uri,
                                         @RequestParam String path,
                                         @RequestParam(defaultValue = "5") int priority) {
        log.info("添加路由: routeId={}, uri={}", routeId, uri);
        return gatewayDemoService.addRoute(routeId, uri, path, priority);
    }

    /**
     * 获取路由
     */
    @GetMapping("/routes/{routeId}")
    public Map<String, Object> getRoute(@PathVariable String routeId) {
        return gatewayDemoService.getRoute(routeId);
    }

    /**
     * 获取所有路由
     */
    @GetMapping("/routes")
    public Map<String, Object> getAllRoutes() {
        return gatewayDemoService.getAllRoutes();
    }

    /**
     * 删除路由
     */
    @DeleteMapping("/routes/{routeId}")
    public Map<String, Object> removeRoute(@PathVariable String routeId) {
        log.info("删除路由: routeId={}", routeId);
        return gatewayDemoService.removeRoute(routeId);
    }

    // ==================== 限流 ====================

    /**
     * 尝试获取限流许可
     */
    @PostMapping("/rate-limit/try-acquire")
    public Map<String, Object> tryAcquire(@RequestParam String key) {
        return gatewayDemoService.tryAcquire(key);
    }

    /**
     * 查询剩余许可数
     */
    @GetMapping("/rate-limit/remaining")
    public Map<String, Object> getRemaining(@RequestParam String key) {
        return gatewayDemoService.getRemaining(key);
    }

    /**
     * 重置限流器
     */
    @DeleteMapping("/rate-limit/reset")
    public Map<String, Object> resetRateLimit(@RequestParam String key) {
        return gatewayDemoService.resetRateLimit(key);
    }

    // ==================== 熔断 ====================

    /**
     * 获取熔断器状态
     */
    @GetMapping("/circuit-breaker/state")
    public Map<String, Object> getCircuitBreakerState() {
        return gatewayDemoService.getCircuitBreakerState();
    }

    /**
     * 重置熔断器
     */
    @PostMapping("/circuit-breaker/reset")
    public Map<String, Object> resetCircuitBreaker() {
        return gatewayDemoService.resetCircuitBreaker();
    }

    /**
     * 执行受熔断保护的操作
     */
    @PostMapping("/circuit-breaker/execute")
    public Map<String, Object> executeWithCircuitBreaker(@RequestParam(defaultValue = "false") boolean shouldFail) {
        log.info("执行熔断保护操作: shouldFail={}", shouldFail);
        return gatewayDemoService.executeWithCircuitBreaker(shouldFail);
    }

    // ==================== 灰度发布 ====================

    /**
     * 灰度路由
     */
    @GetMapping("/canary/route")
    public Map<String, Object> canaryRoute(@RequestParam(required = false) String userId,
                                           @RequestParam(defaultValue = "/api/test") String path,
                                           @RequestParam(defaultValue = "127.0.0.1") String clientIp) {
        return gatewayDemoService.canaryRoute(userId, path, clientIp);
    }

    /**
     * 获取灰度配置
     */
    @GetMapping("/canary/config")
    public Map<String, Object> getCanaryConfig() {
        return gatewayDemoService.getCanaryConfig();
    }

    /**
     * 更新灰度权重
     */
    @PutMapping("/canary/weight")
    public Map<String, Object> updateCanaryWeight(@RequestParam int weight) {
        log.info("更新灰度权重: weight={}", weight);
        return gatewayDemoService.updateCanaryWeight(weight);
    }

    /**
     * 灰度用户管理
     */
    @PostMapping("/canary/users")
    public Map<String, Object> manageCanaryUser(@RequestParam String userId,
                                               @RequestParam(defaultValue = "true") boolean add) {
        log.info("灰度用户管理: userId={}, add={}", userId, add);
        return gatewayDemoService.manageCanaryUser(userId, add);
    }

    // ==================== API 聚合 ====================

    /**
     * 执行 API 聚合
     */
    @PostMapping("/aggregate")
    public Map<String, Object> aggregate(@RequestBody List<AggregateRequest> requests) {
        log.info("API 聚合: {} 个请求", requests.size());
        return gatewayDemoService.aggregateApis(requests);
    }

    /**
     * 执行单个 API 请求
     */
    @PostMapping("/aggregate/single")
    public Map<String, Object> executeSingleApi(@RequestParam String url,
                                                @RequestParam(defaultValue = "GET") String method) {
        return gatewayDemoService.executeSingleApi(url, method);
    }
}
