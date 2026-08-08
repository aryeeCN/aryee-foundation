package cn.aryee.examples.gateway.reactive.service;

import cn.aryee.gateway.api.aggregate.*;
import cn.aryee.gateway.api.canary.*;
import cn.aryee.gateway.api.circuit.*;
import cn.aryee.gateway.api.rate.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;

/**
 * Gateway Reactive 示例服务
 * 演示响应式网关功能：限流、熔断、灰度发布、API 聚合
 * <p>
 * 路由管理由 {@link cn.aryee.gateway.api.route.DynamicRouteService} 提供（详见 DynamicRouteController）。
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayReactiveDemoService {

    private final RateLimiter rateLimiter;
    private final CircuitBreaker circuitBreaker;
    private final CanaryRouter canaryRouter;
    private final ApiAggregator apiAggregator;

    // ========== 限流 ==========

    /**
     * 尝试获取限流许可
     */
    public Mono<Map<String, Object>> tryAcquire(String key) {
        return Mono.fromCallable(() -> {
            RateLimitResult result = rateLimiter.tryAcquire(key);
            Map<String, Object> map = new HashMap<>();
            map.put("allowed", result.isAllowed());
            map.put("remaining", result.getRemaining());
            map.put("limit", result.getLimit());
            map.put("retryAfterMs", result.getRetryAfter());
            map.put("key", result.getKey());
            return map;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 获取剩余许可数
     */
    public Mono<Map<String, Object>> getRemaining(String key) {
        return Mono.fromCallable(() -> {
            long remaining = rateLimiter.getRemaining(key);
            RateLimitConfig config = rateLimiter.getConfig();
            Map<String, Object> map = new HashMap<>();
            map.put("key", key);
            map.put("remaining", remaining);
            map.put("algorithm", config.getAlgorithm());
            map.put("capacity", config.getCapacity());
            return map;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // ========== 熔断 ==========

    /**
     * 获取熔断器状态
     */
    public Mono<Map<String, Object>> getCircuitBreakerState() {
        return Mono.fromCallable(() -> {
            CircuitState state = circuitBreaker.getState();
            CircuitMetrics metrics = circuitBreaker.getMetrics();
            Map<String, Object> map = new HashMap<>();
            map.put("state", state);
            map.put("successCount", metrics.getSuccessCount());
            map.put("failureCount", metrics.getFailureCount());
            map.put("failureRate", metrics.getFailureRate());
            map.put("totalCalls", metrics.getTotalCalls());
            return map;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 重置熔断器
     */
    public Mono<Map<String, Object>> resetCircuitBreaker() {
        return Mono.fromCallable(() -> {
            circuitBreaker.reset();
            Map<String, Object> map = new HashMap<>();
            map.put("success", true);
            map.put("message", "熔断器已重置");
            map.put("state", circuitBreaker.getState());
            return map;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 执行受熔断保护的操作
     */
    public Mono<Map<String, Object>> executeWithCircuitBreaker(boolean shouldFail) {
        return Mono.fromCallable(() -> {
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
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // ========== 灰度发布 ==========

    /**
     * 灰度路由
     */
    public Mono<Map<String, Object>> canaryRoute(String userId, String path, String clientIp) {
        return Mono.fromCallable(() -> {
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
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 获取灰度配置
     */
    public Mono<Map<String, Object>> getCanaryConfig() {
        return Mono.fromCallable(() -> {
            CanaryConfig config = canaryRouter.getConfig();
            Map<String, Object> map = new HashMap<>();
            map.put("weight", config.getWeight());
            map.put("mainUrl", config.getMainUrl());
            map.put("canaryUrl", config.getCanaryUrl());
            map.put("canaryUsers", config.getCanaryUsers());
            map.put("userBasedEnabled", config.isUserBasedEnabled());
            return map;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 更新灰度权重
     */
    public Mono<Map<String, Object>> updateCanaryWeight(int weight) {
        return Mono.fromCallable(() -> {
            canaryRouter.updateWeight(weight);
            Map<String, Object> map = new HashMap<>();
            map.put("success", true);
            map.put("newWeight", weight);
            map.put("currentWeight", canaryRouter.getConfig().getWeight());
            return map;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 灰度用户管理
     */
    public Mono<Map<String, Object>> manageCanaryUser(String userId, boolean add) {
        return Mono.fromCallable(() -> {
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
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // ========== API 聚合 ==========

    /**
     * 执行 API 聚合
     */
    public Mono<Map<String, Object>> aggregateApis(List<AggregateRequest> requests) {
        return Mono.fromCallable(() -> {
            AggregateResponse response = apiAggregator.aggregate(requests);
            Map<String, Object> map = new HashMap<>();
            map.put("success", response.isSuccess());
            map.put("successCount", response.getSuccessCount());
            map.put("failureCount", response.getFailureCount());
            map.put("totalTimeMs", response.getTotalTimeMs());
            map.put("responses", response.getResponses());
            map.put("errors", response.getErrors());
            return map;
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
