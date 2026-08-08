package cn.aryee.examples.cache.reactive.service;

import cn.aryee.cache.api.service.ReactiveCacheService;
import cn.aryee.examples.cache.reactive.model.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 响应式编程式缓存使用示例。
 * <p>
 * 直接注入 {@link ReactiveCacheService} 进行非阻塞缓存读写，
 * 所有操作返回 {@link Mono}，适用于 WebFlux 响应式编程模型。
 * </p>
 *
 * <h2>与 Blocking 模式的对比</h2>
 * <ul>
 *   <li>Blocking: {@code cacheService.get(key)} → 返回 {@code T}</li>
 *   <li>Reactive: {@code reactiveCacheService.get(key)} → 返回 {@code Mono<T>}</li>
 *   <li>禁止在 Reactive 链中调用 {@code Mono.block()}，否则会阻塞事件循环线程</li>
 * </ul>
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReactiveProgrammaticCacheService {

    private final ReactiveCacheService reactiveCacheService;
    private final AtomicLong idGenerator = new AtomicLong(1L);

    private static final String KEY_PREFIX = "reactive:user:";

    // ==================== 基本 String 缓存 ====================

    /**
     * 创建用户并缓存（非阻塞）。
     * 演示 {@link ReactiveCacheService#set(String, Object, Duration)}。
     */
    public Mono<UserDTO> createUser(String username, String email) {
        UserDTO user = UserDTO.builder()
                .id(idGenerator.getAndIncrement())
                .username(username)
                .email(email)
                .status(1)
                .createTime(LocalDateTime.now())
                .build();

        return reactiveCacheService.set(KEY_PREFIX + user.getId(), user, Duration.ofMinutes(10))
                .doOnSuccess(v -> log.info("[Reactive] 创建用户并缓存: id={}, username={}", user.getId(), username))
                .thenReturn(user);
    }

    /**
     * 查询用户（先查缓存，未命中则回源）。
     * <p>
     * 响应式链式操作：cache.get → 如果为空则回源 → 回填缓存。
     * 全程非阻塞，不使用 {@code block()}。
     * </p>
     */
    public Mono<UserDTO> getUser(Long id) {
        String key = KEY_PREFIX + id;
        return reactiveCacheService.<UserDTO>get(key)
                .doOnNext(user -> log.info("[Reactive] 缓存命中: id={}", id))
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("[Reactive] 缓存未命中，回源查询: id={}", id);
                    UserDTO dbUser = mockDbQuery(id);
                    if (dbUser != null) {
                        return reactiveCacheService.set(key, dbUser, Duration.ofMinutes(5))
                                .thenReturn(dbUser);
                    }
                    // 缓存空值防止穿透
                    return reactiveCacheService.set(key, "NULL", Duration.ofSeconds(30))
                            .doOnSuccess(v -> log.info("[Reactive] 写入空值标记防止穿透: id={}", id))
                            .then(Mono.empty());
                }));
    }

    /**
     * 删除用户缓存。
     */
    public Mono<Boolean> deleteUser(Long id) {
        return reactiveCacheService.delete(KEY_PREFIX + id)
                .doOnNext(deleted -> log.info("[Reactive] 删除缓存: id={}, result={}", id, deleted));
    }

    /**
     * 检查 key 是否存在。
     */
    public Mono<Boolean> exists(Long id) {
        return reactiveCacheService.hasKey(KEY_PREFIX + id);
    }

    // ==================== TTL 管理 ====================

    /**
     * 获取缓存剩余过期时间。
     */
    public Mono<Duration> getRemainingTtl(Long id) {
        return reactiveCacheService.getExpire(KEY_PREFIX + id);
    }

    /**
     * 续期缓存。
     */
    public Mono<Boolean> renewTtl(Long id, Duration duration) {
        return reactiveCacheService.expire(KEY_PREFIX + id, duration);
    }

    // ==================== 批量操作 ====================

    /**
     * 批量查询用户。
     * 演示 {@link ReactiveCacheService#multiGet(List)}。
     */
    public Mono<Map<String, UserDTO>> multiGet(List<Long> ids) {
        List<String> keys = ids.stream().map(id -> KEY_PREFIX + id).toList();
        return reactiveCacheService.<UserDTO>multiGet(keys)
                .doOnNext(result -> log.info("[Reactive] 批量查询: 请求 {} 个, 命中 {} 个", ids.size(), result.size()));
    }

    /**
     * 批量设置缓存。
     */
    public Mono<Void> multiSet(Map<Long, UserDTO> users) {
        Map<String, Object> cacheMap = new HashMap<>();
        users.forEach((id, user) -> cacheMap.put(KEY_PREFIX + id, user));
        return reactiveCacheService.multiSet(cacheMap)
                .doOnSuccess(v -> log.info("[Reactive] 批量设置缓存: {} 条", users.size()));
    }

    // ==================== 计数器（限流场景） ====================

    /**
     * 访问计数器（限流场景）。
     * 演示 {@link ReactiveCacheService#increment(String, long)}。
     */
    public Mono<Long> recordAccess(Long userId) {
        String countKey = "reactive:access:count:" + userId;
        return reactiveCacheService.increment(countKey, 1)
                .flatMap(count -> {
                    if (count == 1) {
                        return reactiveCacheService.expire(countKey, Duration.ofMinutes(1))
                                .thenReturn(count);
                    }
                    return Mono.just(count);
                })
                .doOnNext(count -> log.info("[Reactive] 用户 {} 访问计数: {}（1分钟窗口）", userId, count));
    }

    // ==================== 私有方法 ====================

    private UserDTO mockDbQuery(Long id) {
        if (id == null || id < 1 || id > 3) {
            return null;
        }
        return UserDTO.builder()
                .id(id)
                .username("reactive_user" + id)
                .email("user" + id + "@example.com")
                .status(1)
                .createTime(LocalDateTime.now().minusDays(id))
                .build();
    }
}
