package cn.aryee.examples.cache.reactive;

import cn.aryee.cache.api.service.ReactiveCacheService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Aryee Cache Reactive 模式 Redis 集成测试
 * 验证 ReactiveRedisCacheServiceImpl 与真实 Redis 服务的连通性和功能正确性
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Redis Cache 集成测试 - Reactive 模式")
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class RedisCacheReactiveIntegrationTest {

    private final ReactiveCacheService reactiveCacheService;

    private static final String KEY_PREFIX = "test:reactive:";

    @BeforeEach
    void setUp() {
        cleanupKeys().block();
    }

    @AfterEach
    void tearDown() {
        cleanupKeys().block();
    }

    private reactor.core.publisher.Mono<Void> cleanupKeys() {
        return reactor.core.publisher.Flux.range(0, 20)
                .flatMap(i -> reactiveCacheService.delete(KEY_PREFIX + i))
                .then();
    }

    @Test
    @DisplayName("ReactiveCacheService 注入验证 - 确认使用 Redis 实现")
    void testReactiveCacheServiceInjection() {
        assertThat(reactiveCacheService).isNotNull();
        assertThat(reactiveCacheService.getClass().getSimpleName())
                .isEqualTo("ReactiveRedisCacheServiceImpl");
    }

    @Test
    @DisplayName("set/get - 基本字符串存取")
    void testSetGetString() {
        String key = KEY_PREFIX + "1";
        String value = "reactive-redis-cache";

        StepVerifier.create(reactiveCacheService.set(key, value))
                .verifyComplete();

        StepVerifier.create(reactiveCacheService.<String>get(key))
                .assertNext(result -> assertThat(result).isEqualTo(value))
                .verifyComplete();
    }

    @Test
    @DisplayName("set 带过期时间 - Duration 模式")
    void testSetWithExpiration() {
        String key = KEY_PREFIX + "2";
        String value = "will-expire-reactive";

        StepVerifier.create(reactiveCacheService.set(key, value, Duration.ofSeconds(5)))
                .verifyComplete();

        StepVerifier.create(reactiveCacheService.hasKey(key))
                .assertNext(exists -> assertThat(exists).isTrue())
                .verifyComplete();

        StepVerifier.create(reactiveCacheService.getExpire(key))
                .assertNext(expire -> {
                    assertThat(expire).isNotNull();
                    assertThat(expire.toSeconds()).isBetween(1L, 5L);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("delete - 删除键")
    void testDelete() {
        String key = KEY_PREFIX + "3";

        StepVerifier.create(reactiveCacheService.set(key, "to-delete").then(reactiveCacheService.delete(key)))
                .assertNext(deleted -> assertThat(deleted).isTrue())
                .verifyComplete();

        StepVerifier.create(reactiveCacheService.hasKey(key))
                .assertNext(exists -> assertThat(exists).isFalse())
                .verifyComplete();
    }

    @Test
    @DisplayName("hasKey - 判断键是否存在")
    void testHasKey() {
        String existKey = KEY_PREFIX + "4";
        String absentKey = KEY_PREFIX + "4-absent";

        StepVerifier.create(reactiveCacheService.set(existKey, "exists")
                        .then(reactiveCacheService.hasKey(existKey)))
                .assertNext(exists -> assertThat(exists).isTrue())
                .verifyComplete();

        StepVerifier.create(reactiveCacheService.hasKey(absentKey))
                .assertNext(exists -> assertThat(exists).isFalse())
                .verifyComplete();
    }

    @Test
    @DisplayName("increment - 原子计数器")
    void testIncrement() {
        String key = KEY_PREFIX + "5";

        StepVerifier.create(reactiveCacheService.increment(key, 1L))
                .assertNext(val -> assertThat(val).isEqualTo(1L))
                .verifyComplete();

        StepVerifier.create(reactiveCacheService.increment(key, 5L))
                .assertNext(val -> assertThat(val).isEqualTo(6L))
                .verifyComplete();

        StepVerifier.create(reactiveCacheService.decrement(key, 2L))
                .assertNext(val -> assertThat(val).isEqualTo(4L))
                .verifyComplete();
    }

    @Test
    @DisplayName("multiSet/multiGet - 批量存取")
    void testMultiSetMultiGet() {
        Map<String, Object> map = new HashMap<>();
        map.put(KEY_PREFIX + "multi1", "value1");
        map.put(KEY_PREFIX + "multi2", "value2");
        map.put(KEY_PREFIX + "multi3", "value3");

        StepVerifier.create(reactiveCacheService.multiSet(map))
                .verifyComplete();

        StepVerifier.create(reactiveCacheService.<String>multiGet(List.of(
                        KEY_PREFIX + "multi1",
                        KEY_PREFIX + "multi2",
                        KEY_PREFIX + "multi3",
                        KEY_PREFIX + "multi-absent"
                )))
                .assertNext(result -> {
                    assertThat(result).hasSize(3);
                    assertThat(result.get(KEY_PREFIX + "multi1")).isEqualTo("value1");
                    assertThat(result.get(KEY_PREFIX + "multi2")).isEqualTo("value2");
                    assertThat(result.get(KEY_PREFIX + "multi3")).isEqualTo("value3");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("multiDelete - 批量删除")
    void testMultiDelete() {
        reactor.core.publisher.Flux.range(1, 3)
                .flatMap(i -> reactiveCacheService.set(KEY_PREFIX + "mdel" + i, "v" + i))
                .blockLast();

        StepVerifier.create(reactiveCacheService.multiDelete(List.of(
                        KEY_PREFIX + "mdel1",
                        KEY_PREFIX + "mdel2",
                        KEY_PREFIX + "mdel-absent"
                )))
                .assertNext(deleted -> assertThat(deleted).isGreaterThanOrEqualTo(2L))
                .verifyComplete();

        StepVerifier.create(reactiveCacheService.hasKey(KEY_PREFIX + "mdel3"))
                .assertNext(exists -> assertThat(exists).isTrue())
                .verifyComplete();
    }

    @Test
    @DisplayName("get 带默认值 - 缺失时返回 default")
    void testGetWithDefault() {
        String key = KEY_PREFIX + "default-test";

        StepVerifier.create(reactiveCacheService.get(key, "default-value"))
                .assertNext(result -> assertThat(result).isEqualTo("default-value"))
                .verifyComplete();
    }
}
