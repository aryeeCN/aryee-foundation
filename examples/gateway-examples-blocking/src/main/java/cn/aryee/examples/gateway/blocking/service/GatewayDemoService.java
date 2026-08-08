package cn.aryee.examples.gateway.blocking.service;

import cn.aryee.gateway.api.aggregate.*;
import cn.aryee.gateway.api.canary.*;
import cn.aryee.gateway.api.circuit.*;
import cn.aryee.gateway.api.rate.*;
import cn.aryee.gateway.api.model.GatewayRoute;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gateway Blocking 示例服务
 * 演示限流、熔断、灰度发布、API 聚合等网关功能
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayDemoService {

    private final RateLimiter rateLimiter;
    private final CircuitBreaker circuitBreaker;
    private final CanaryRouter canaryRouter;
    private final ApiAggregator apiAggregator;

    // ========== 路由管理 ==========

    private final Map<String, GatewayRoute> routeStore = new ConcurrentHashMap<>();

    /**
     * 添加路由
     */
    public Map<String, Object> addRoute(String routeId, String uri, String path, int priority) {
        GatewayRoute route = GatewayRoute.builder()
                .routeId(routeId)
                .uri(uri)
                .path(path)
                .priority(priority)
                .enabled(true)
                .createTime(LocalDateTime.now())
                .build();
        routeStore.put(routeId, route);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("route", route);
        return result;
    }

    /**
     * 获取路由
     */
    public Map<String, Object> getRoute(String routeId) {
        GatewayRoute route = routeStore.get(routeId);
        Map<String, Object> result = new HashMap<>();
        result.put("found", route != null);
        result.put("route", route);
        return result;
    }

    /**
     * 获取所有路由
     */
    public Map<String, Object> getAllRoutes() {
        Map<String, Object> result = new HashMap<>();
        result.put("total", routeStore.size());
        result.put("routes", new ArrayList<>(routeStore.values()));
        return result;
    }

    /**
     * 删除路由
     */
    public Map<String, Object> removeRoute(String routeId) {
        GatewayRoute removed = routeStore.remove(routeId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", removed != null);
        result.put("removedRouteId", routeId);
        return result;
    }

    // ========== 限流 ==========

    /**
     * 尝试获取限流许可
     */
    public Map<String, Object> tryAcquire(String key) {
        RateLimitResult result = rateLimiter.tryAcquire(key);
        Map<String, Object> map = new HashMap<>();
        map.put("allowed", result.isAllowed());
        map.put("remaining", result.getRemaining());
        map.put("limit", result.getLimit());
        map.put("retryAfterMs", result.getRetryAfter());
        map.put("key", result.getKey());
        return map;
    }

    /**
     * 获取剩余许可数
     */
    public Map<String, Object> getRemaining(String key) {
        long remaining = rateLimiter.getRemaining(key);
        RateLimitConfig config = rateLimiter.getConfig();
        Map<String, Object> map = new HashMap<>();
        map.put("key", key);
        map.put("remaining", remaining);
        map.put("algorithm", config.getAlgorithm());
        map.put("capacity", config.getCapacity());
        return map;
    }

    /**
     * 重置限流器
     */
    public Map<String, Object> resetRateLimit(String key) {
        rateLimiter.reset(key);
        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        map.put("message", "限流器已重置");
        map.put("key", key);
        return map;
    }

    // ========== 熔断 ==========

    /**
     * 获取熔断器状态
     */
    public Map<String, Object> getCircuitBreakerState() {
        CircuitState state = circuitBreaker.getState();
        CircuitMetrics metrics = circuitBreaker.getMetrics();
        Map<String, Object> map = new HashMap<>();
        map.put("state", state);
        map.put("successCount", metrics.getSuccessCount());
        map.put("failureCount", metrics.getFailureCount());
        map.put("failureRate", metrics.getFailureRate());
        map.put("totalCalls", metrics.getTotalCalls());
        map.put("notPermittedCount", metrics.getNotPermittedCount());
        return map;
    }

    /**
     * 重置熔断器
     */
    public Map<String, Object> resetCircuitBreaker() {
        circuitBreaker.reset();
        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        map.put("message", "熔断器已重置");
        map.put("state", circuitBreaker.getState());
        return map;
    }

    /**
     * 执行受熔断保护的模拟操作
     */
    public Map<String, Object> executeWithCircuitBreaker(boolean shouldFail) {
        Map<String, Object> result = new HashMap<>();
        try {
            String data = circuitBreaker.execute(
                    () -> {
                        if (shouldFail) {
                            throw new RuntimeException("模拟业务异常");
                        }
                        return "操作成功";
                    },
                    () -> "降级响应"
            );
            result.put("data", data);
            result.put("circuitState", circuitBreaker.getState());
        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("circuitState", circuitBreaker.getState());
        }
        return result;
    }

    // ========== 灰度发布 ==========

    /**
     * 灰度路由
     */
    public Map<String, Object> canaryRoute(String userId, String path, String clientIp) {
        RequestContext context = RequestContext.builder()
                .userId(userId)
                .path(path)
                .clientIp(clientIp)
                .headers(new HashMap<>())
                .parameters(new HashMap<>())
                .build();

        RouteDestination destination = canaryRouter.route(context);
        Map<String, Object> map = new HashMap<>();
        map.put("destinationType", destination.getType());
        map.put("targetUrl", destination.getTargetUrl());
        map.put("version", destination.getVersion());
        map.put("userId", userId);
        return map;
    }

    /**
     * 获取灰度配置
     */
    public Map<String, Object> getCanaryConfig() {
        CanaryConfig config = canaryRouter.getConfig();
        Map<String, Object> map = new HashMap<>();
        map.put("weight", config.getWeight());
        map.put("mainUrl", config.getMainUrl());
        map.put("canaryUrl", config.getCanaryUrl());
        map.put("canaryUsers", config.getCanaryUsers());
        map.put("userBasedEnabled", config.isUserBasedEnabled());
        return map;
    }

    /**
     * 更新灰度权重
     */
    public Map<String, Object> updateCanaryWeight(int weight) {
        canaryRouter.updateWeight(weight);
        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        map.put("newWeight", weight);
        map.put("currentWeight", canaryRouter.getConfig().getWeight());
        return map;
    }

    /**
     * 灰度用户管理
     */
    public Map<String, Object> manageCanaryUser(String userId, boolean add) {
        if (add) {
            canaryRouter.addCanaryUser(userId);
        } else {
            canaryRouter.removeCanaryUser(userId);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("action", add ? "add" : "remove");
        map.put("userId", userId);
        map.put("isCanaryUser", canaryRouter.isCanaryUser(userId));
        map.put("canaryUsers", canaryRouter.getCanaryUsers());
        return map;
    }

    // ========== API 聚合 ==========

    /**
     * 执行 API 聚合
     */
    public Map<String, Object> aggregateApis(List<AggregateRequest> requests) {
        AggregateResponse response = apiAggregator.aggregate(requests);
        Map<String, Object> map = new HashMap<>();
        map.put("success", response.isSuccess());
        map.put("successCount", response.getSuccessCount());
        map.put("failureCount", response.getFailureCount());
        map.put("totalTimeMs", response.getTotalTimeMs());
        map.put("responses", response.getResponses());
        map.put("errors", response.getErrors());
        return map;
    }

    /**
     * 执行单个 API 请求
     */
    public Map<String, Object> executeSingleApi(String url, String method) {
        AggregateRequest request = AggregateRequest.builder()
                .id("req-" + System.currentTimeMillis())
                .url(url)
                .method(method)
                .timeoutMs(5000)
                .build();

        ApiResponse response = apiAggregator.execute(request);
        Map<String, Object> map = new HashMap<>();
        map.put("success", response.isSuccess());
        map.put("statusCode", response.getStatusCode());
        map.put("body", response.getBody());
        map.put("timeMs", response.getTimeMs());
        map.put("errorMessage", response.getErrorMessage());
        map.put("usedFallback", response.isUsedFallback());
        return map;
    }
}
