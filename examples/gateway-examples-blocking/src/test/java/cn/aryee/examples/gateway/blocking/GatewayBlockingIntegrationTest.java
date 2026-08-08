package cn.aryee.examples.gateway.blocking;

import cn.aryee.gateway.api.aggregate.ApiAggregator;
import cn.aryee.gateway.api.canary.CanaryRouter;
import cn.aryee.gateway.api.circuit.CircuitBreaker;
import cn.aryee.gateway.api.circuit.CircuitState;
import cn.aryee.gateway.api.rate.RateLimiter;
import cn.aryee.gateway.api.rate.RateLimitResult;
import cn.aryee.gateway.autoconfigure.GatewayProperties;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gateway Blocking 集成测试
 * 测试限流、熔断、灰度发布、API 聚合等网关功能
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootTest
@DisplayName("Gateway Blocking 集成测试")
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
public class GatewayBlockingIntegrationTest {

    private final GatewayProperties gatewayProperties;

    private final RateLimiter rateLimiter;

    private final CircuitBreaker circuitBreaker;

    private final CanaryRouter canaryRouter;

    private final ApiAggregator apiAggregator;

    @Test
    @DisplayName("Gateway 配置加载测试")
    void testGatewayPropertiesLoaded() {
        assertNotNull(gatewayProperties, "GatewayProperties 应该被加载");
        assertTrue(gatewayProperties.isEnabled(), "Gateway 模块应该启用");
        assertTrue(gatewayProperties.getRateLimit().isEnabled(), "限流应该启用");
        assertTrue(gatewayProperties.getCircuitBreaker().isEnabled(), "熔断应该启用");
        assertTrue(gatewayProperties.getCanary().isEnabled(), "灰度应该启用");
        assertTrue(gatewayProperties.getAggregate().isEnabled(), "聚合应该启用");
    }

    @Test
    @DisplayName("限流器获取许可测试")
    void testRateLimiterAcquire() {
        String key = "test-api-" + System.currentTimeMillis();
        RateLimitResult result = rateLimiter.tryAcquire(key);
        assertNotNull(result, "限流结果不应为 null");
        assertTrue(result.isAllowed(), "首次获取应该允许");
        assertTrue(result.getRemaining() >= 0, "剩余许可数应 >= 0");
    }

    @Test
    @DisplayName("限流器多次获取测试")
    void testRateLimiterMultipleAcquire() {
        String key = "multi-acquire-" + System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
            RateLimitResult result = rateLimiter.tryAcquire(key);
            assertNotNull(result);
        }
        long remaining = rateLimiter.getRemaining(key);
        assertTrue(remaining >= 0, "剩余许可数应 >= 0");
    }

    @Test
    @DisplayName("限流器重置测试")
    void testRateLimiterReset() {
        String key = "reset-test-" + System.currentTimeMillis();
        rateLimiter.tryAcquire(key);
        rateLimiter.reset(key);
        long remaining = rateLimiter.getRemaining(key);
        assertTrue(remaining >= 0, "重置后剩余许可数应 >= 0");
    }

    @Test
    @DisplayName("熔断器初始状态测试")
    void testCircuitBreakerInitialState() {
        CircuitState state = circuitBreaker.getState();
        assertNotNull(state, "熔断器状态不应为 null");
        assertEquals(CircuitState.CLOSED, state, "初始状态应为 CLOSED");
    }

    @Test
    @DisplayName("熔断器执行成功操作测试")
    void testCircuitBreakerExecuteSuccess() {
        String result = circuitBreaker.execute(
                () -> "成功操作",
                () -> "降级响应"
        );
        assertEquals("成功操作", result, "应返回正常操作结果");
    }

    @Test
    @DisplayName("熔断器执行降级操作测试")
    void testCircuitBreakerExecuteFallback() {
        circuitBreaker.execute(
                () -> { throw new RuntimeException("模拟失败"); },
                () -> null
        );
        // 执行后检查状态仍然可用
        assertNotNull(circuitBreaker.getState());
    }

    @Test
    @DisplayName("熔断器指标测试")
    void testCircuitBreakerMetrics() {
        circuitBreaker.execute(() -> "测试", () -> "降级");
        var metrics = circuitBreaker.getMetrics();
        assertNotNull(metrics, "指标不应为 null");
        assertTrue(metrics.getTotalCalls() >= 0, "总调用次数应 >= 0");
    }

    @Test
    @DisplayName("熔断器重置测试")
    void testCircuitBreakerReset() {
        circuitBreaker.reset();
        assertEquals(CircuitState.CLOSED, circuitBreaker.getState(), "重置后状态应为 CLOSED");
    }

    @Test
    @DisplayName("灰度路由配置测试")
    void testCanaryConfig() {
        var config = canaryRouter.getConfig();
        assertNotNull(config, "灰度配置不应为 null");
        assertTrue(config.getWeight() >= 0 && config.getWeight() <= 100, "权重应在 0-100 之间");
        assertNotNull(config.getMainUrl(), "主版本 URL 不应为 null");
        assertNotNull(config.getCanaryUrl(), "灰度版本 URL 不应为 null");
    }

    @Test
    @DisplayName("灰度路由决策测试")
    void testCanaryRoute() {
        var context = cn.aryee.gateway.api.canary.RequestContext.builder()
                .userId("user-001")
                .path("/api/test")
                .clientIp("127.0.0.1")
                .headers(new java.util.HashMap<>())
                .parameters(new java.util.HashMap<>())
                .build();
        var destination = canaryRouter.route(context);
        assertNotNull(destination, "路由目标不应为 null");
        assertNotNull(destination.getType(), "目标类型不应为 null");
    }

    @Test
    @DisplayName("灰度用户管理测试")
    void testCanaryUserManagement() {
        String testUser = "test-canary-user-" + System.currentTimeMillis();
        canaryRouter.addCanaryUser(testUser);
        assertTrue(canaryRouter.isCanaryUser(testUser), "添加后应为灰度用户");

        canaryRouter.removeCanaryUser(testUser);
        assertFalse(canaryRouter.isCanaryUser(testUser), "移除后不应为灰度用户");
    }

    @Test
    @DisplayName("灰度权重更新测试")
    void testCanaryWeightUpdate() {
        int originalWeight = canaryRouter.getConfig().getWeight();
        canaryRouter.updateWeight(50);
        assertEquals(50, canaryRouter.getConfig().getWeight(), "权重应更新为 50");
        canaryRouter.updateWeight(originalWeight);
    }

    @Test
    @DisplayName("API 聚合器配置测试")
    void testApiAggregatorConfig() {
        var config = apiAggregator.getConfig();
        assertNotNull(config, "聚合配置不应为 null");
        assertNotNull(config.getExecutionMode(), "执行模式不应为 null");
        assertTrue(config.getDefaultTimeoutMs() > 0, "默认超时应 > 0");
    }
}
