package cn.aryee.examples.cache.blocking;

import cn.aryee.cache.api.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Aryee Cache Blocking 模式 Redis 集成测试
 * 验证 RedisCacheServiceImpl 与真实 Redis 服务的连通性和功能正确性
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Redis Cache 集成测试 - Blocking 模式")
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class RedisCacheBlockingIntegrationTest {

    private final CacheService cacheService;

    private static final String KEY_PREFIX = "test:blocking:";

    @BeforeEach
    void setUp() {
        cleanupKeys();
    }

    @AfterEach
    void tearDown() {
        cleanupKeys();
    }

    private void cleanupKeys() {
        for (int i = 0; i < 20; i++) {
            cacheService.delete(KEY_PREFIX + i);
            cacheService.delete(KEY_PREFIX + "hash:" + i);
            cacheService.delete(KEY_PREFIX + "set:" + i);
            cacheService.delete(KEY_PREFIX + "list:" + i);
        }
    }

    @Test
    @DisplayName("set/get - 基本字符串存取")
    void testSetGetString() {
        String key = KEY_PREFIX + "1";
        String value = "hello-redis-cache";

        cacheService.set(key, value);
        String result = cacheService.get(key);

        assertThat(result).isEqualTo(value);
    }

    @Test
    @DisplayName("set/get - 对象存取")
    void testSetGetObject() {
        String key = KEY_PREFIX + "2";
        Map<String, Object> user = new HashMap<>();
        user.put("id", 1001);
        user.put("name", "Aryee");
        user.put("active", true);

        cacheService.set(key, user);
        Map<String, Object> result = cacheService.get(key);

        assertThat(result).isNotNull();
        assertThat(result.get("id")).isEqualTo(1001);
        assertThat(result.get("name")).isEqualTo("Aryee");
        assertThat(result.get("active")).isEqualTo(true);
    }

    @Test
    @DisplayName("set 带过期时间 - Duration 模式")
    void testSetWithExpiration() throws InterruptedException {
        String key = KEY_PREFIX + "3";
        String value = "will-expire";

        cacheService.set(key, value, Duration.ofSeconds(2));

        assertThat(cacheService.hasKey(key)).isTrue();
        String cachedValue = cacheService.get(key);
        assertThat(cachedValue).isEqualTo(value);

        Duration expire = cacheService.getExpire(key);
        assertThat(expire).isNotNull();
        assertThat(expire.toSeconds()).isBetween(1L, 2L);

        Thread.sleep(2500);
        assertThat(cacheService.hasKey(key)).isFalse();
        String expiredValue = cacheService.get(key);
        assertThat(expiredValue).isNull();
    }

    @Test
    @DisplayName("delete - 删除键")
    void testDelete() {
        String key = KEY_PREFIX + "4";
        cacheService.set(key, "to-be-deleted");

        assertThat(cacheService.hasKey(key)).isTrue();
        boolean deleted = cacheService.delete(key);

        assertThat(deleted).isTrue();
        assertThat(cacheService.hasKey(key)).isFalse();
    }

    @Test
    @DisplayName("hasKey - 判断键是否存在")
    void testHasKey() {
        String existKey = KEY_PREFIX + "5";
        String absentKey = KEY_PREFIX + "5-absent";

        cacheService.set(existKey, "exists");

        assertThat(cacheService.hasKey(existKey)).isTrue();
        assertThat(cacheService.hasKey(absentKey)).isFalse();
    }

    @Test
    @DisplayName("expire - 设置已存在键的过期时间")
    void testExpire() {
        String key = KEY_PREFIX + "6";
        cacheService.set(key, "no-ttl");

        boolean result = cacheService.expire(key, Duration.ofSeconds(60));
        assertThat(result).isTrue();

        Duration expire = cacheService.getExpire(key);
        assertThat(expire).isNotNull();
        assertThat(expire.toSeconds()).isBetween(1L, 60L);
    }

    @Test
    @DisplayName("increment/decrement - 原子计数器")
    void testIncrementDecrement() {
        String key = KEY_PREFIX + "7";

        long afterInc1 = cacheService.increment(key, 1);
        assertThat(afterInc1).isEqualTo(1L);

        long afterInc5 = cacheService.increment(key, 5);
        assertThat(afterInc5).isEqualTo(6L);

        long afterDec2 = cacheService.decrement(key, 2);
        assertThat(afterDec2).isEqualTo(4L);
    }

    @Test
    @DisplayName("multiSet/multiGet - 批量存取")
    void testMultiSetMultiGet() {
        Map<String, Object> map = new HashMap<>();
        map.put(KEY_PREFIX + "multi1", "value1");
        map.put(KEY_PREFIX + "multi2", "value2");
        map.put(KEY_PREFIX + "multi3", "value3");

        cacheService.multiSet(map);

        Map<String, String> result = cacheService.multiGet(List.of(
                KEY_PREFIX + "multi1",
                KEY_PREFIX + "multi2",
                KEY_PREFIX + "multi3",
                KEY_PREFIX + "multi-absent"
        ));

        assertThat(result).hasSize(3);
        assertThat(result.get(KEY_PREFIX + "multi1")).isEqualTo("value1");
        assertThat(result.get(KEY_PREFIX + "multi2")).isEqualTo("value2");
        assertThat(result.get(KEY_PREFIX + "multi3")).isEqualTo("value3");
        assertThat(result.containsKey(KEY_PREFIX + "multi-absent")).isFalse();
    }

    @Test
    @DisplayName("multiDelete - 批量删除")
    void testMultiDelete() {
        cacheService.set(KEY_PREFIX + "mdel1", "v1");
        cacheService.set(KEY_PREFIX + "mdel2", "v2");
        cacheService.set(KEY_PREFIX + "mdel3", "v3");

        long deleted = cacheService.multiDelete(List.of(
                KEY_PREFIX + "mdel1",
                KEY_PREFIX + "mdel2",
                KEY_PREFIX + "mdel-absent"
        ));

        // multiDelete 内部会同时删除业务键和互斥锁键，返回值 = Redis实际删除数 / 2
        // 简单 set 不会创建互斥锁键，所以 2 个业务键被删除 → Redis返回2 → multiDelete返回 2/2=1
        assertThat(deleted).isEqualTo(1L);
        assertThat(cacheService.hasKey(KEY_PREFIX + "mdel1")).isFalse();
        assertThat(cacheService.hasKey(KEY_PREFIX + "mdel2")).isFalse();
        assertThat(cacheService.hasKey(KEY_PREFIX + "mdel3")).isTrue();
    }

    @Test
    @DisplayName("hSet/hGet/hGetAll - Hash 存取")
    void testHashOperations() {
        String key = KEY_PREFIX + "hash:1";

        cacheService.hSet(key, "field1", "value1");
        cacheService.hSet(key, "field2", "value2");
        cacheService.hSet(key, "count", 42);

        String hashField1 = cacheService.hGet(key, "field1");
        String hashField2 = cacheService.hGet(key, "field2");
        assertThat(hashField1).isEqualTo("value1");
        assertThat(hashField2).isEqualTo("value2");

        Map<String, Object> all = cacheService.hGetAll(key);
        assertThat(all).hasSize(3);
        assertThat(all.get("field1")).isEqualTo("value1");
        assertThat(all.get("field2")).isEqualTo("value2");
    }

    @Test
    @DisplayName("hDelete - Hash 字段删除")
    void testHashDelete() {
        String key = KEY_PREFIX + "hash:2";

        cacheService.hSet(key, "f1", "v1");
        cacheService.hSet(key, "f2", "v2");

        boolean deleted = cacheService.hDelete(key, "f1");
        assertThat(deleted).isTrue();
        assertThat(cacheService.hExists(key, "f1")).isFalse();
        assertThat(cacheService.hExists(key, "f2")).isTrue();
    }

    @Test
    @DisplayName("hIncrement - Hash 原子计数")
    void testHashIncrement() {
        String key = KEY_PREFIX + "hash:3";

        cacheService.hSet(key, "counter", 10);
        long result = cacheService.hIncrement(key, "counter", 5);
        assertThat(result).isEqualTo(15L);
    }

    @Test
    @DisplayName("sAdd/sMembers/sIsMember/sSize - Set 操作")
    void testSetOperations() {
        String key = KEY_PREFIX + "set:1";

        long added1 = cacheService.sAdd(key, "member1", "member2", "member3");
        assertThat(added1).isEqualTo(3L);

        Set<String> members = cacheService.sMembers(key);
        assertThat(members).containsExactlyInAnyOrder("member1", "member2", "member3");

        assertThat(cacheService.sIsMember(key, "member1")).isTrue();
        assertThat(cacheService.sIsMember(key, "absent")).isFalse();

        assertThat(cacheService.sSize(key)).isEqualTo(3L);

        long removed = cacheService.sRemove(key, "member1");
        assertThat(removed).isEqualTo(1L);
        assertThat(cacheService.sSize(key)).isEqualTo(2L);
    }

    @Test
    @DisplayName("lPush/lRange/lPop/lSize - List 操作")
    void testListOperations() {
        String key = KEY_PREFIX + "list:1";

        cacheService.lPush(key, "item1");
        cacheService.lPush(key, "item2");
        cacheService.rPush(key, "item3");

        assertThat(cacheService.lSize(key)).isEqualTo(3L);

        List<String> range = cacheService.lRange(key, 0, -1);
        assertThat(range).hasSize(3);

        String popped = cacheService.lPop(key);
        assertThat(popped).isIn("item1", "item2", "item3");
        assertThat(cacheService.lSize(key)).isEqualTo(2L);
    }

    @Test
    @DisplayName("get 带默认值 - 缺失时返回 default")
    void testGetWithDefault() {
        String key = KEY_PREFIX + "default-test";
        String result = cacheService.get(key, "default-value");
        assertThat(result).isEqualTo("default-value");
    }

    @Test
    @DisplayName("CacheService 注入验证 - 确认使用 Redis 实现")
    void testCacheServiceInjection() {
        assertThat(cacheService).isNotNull();
        assertThat(cacheService.getClass().getSimpleName()).isEqualTo("RedisCacheServiceImpl");
    }
}
