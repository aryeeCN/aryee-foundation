package cn.aryee.examples.cache.blocking.service;

import cn.aryee.examples.cache.blocking.model.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 声明式缓存使用示例。
 * <p>
 * 使用 Spring 标准的 {@code @Cacheable} / {@code @CachePut} / {@code @CacheEvict} 注解，
 * 底层通过 {@code AryeeCacheManager} 桥接到 Aryee {@link cn.aryee.cache.api.service.CacheService}。
 * </p>
 *
 * <h2>三种注解对比</h2>
 * <ul>
 *   <li>{@code @Cacheable} — 方法执行前查缓存，命中则跳过方法体；未命中则执行方法并缓存结果</li>
 *   <li>{@code @CachePut} — 始终执行方法体，将返回值更新到缓存（适用于写操作后刷新缓存）</li>
 *   <li>{@code @CacheEvict} — 删除缓存条目（适用于删除/更新场景）</li>
 * </ul>
 *
 * <h2>缓存穿透/击穿/雪崩防护</h2>
 * <ul>
 *   <li>穿透：{@code unless = "#result == null"} 控制是否缓存空值</li>
 *   <li>击穿：在 Aryee CacheProperties 中启用 {@code sync=true}（互斥锁）</li>
 *   <li>雪崩：在 Aryee CacheProperties 中启用 {@code randomizeTtl=true}（随机 TTL）</li>
 * </ul>
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@Service
public class DeclarativeCacheService {

    private final AtomicLong idGenerator = new AtomicLong(100L);

    /**
     * 查询用户（带缓存）。
     * <p>
     * cacheNames = "users" 对应 {@code AryeeCacheManager.getCache("users")}，
     * key 使用 SpEL 表达式 {@code #id} 引用方法参数。
     * </p>
     *
     * @param id 用户 ID
     * @return 用户信息（未找到返回 null）
     */
    @Cacheable(cacheNames = "users", key = "#id", unless = "#result == null")
    public UserDTO getUserById(Long id) {
        log.info("[声明式] 方法体执行，模拟查询数据库: id={}", id);
        return mockDbQuery(id);
    }

    /**
     * 创建用户并写入缓存。
     * <p>
     * {@code @CachePut} 始终执行方法体，将返回值写入缓存。
     * 适用于「先写库，再刷新缓存」场景。
     * </p>
     */
    @CachePut(cacheNames = "users", key = "#result.id")
    public UserDTO createUser(String username, String email) {
        UserDTO user = UserDTO.builder()
                .id(idGenerator.getAndIncrement())
                .username(username)
                .email(email)
                .status(1)
                .createTime(LocalDateTime.now())
                .build();
        log.info("[声明式] 创建用户并写入缓存: id={}, username={}", user.getId(), username);
        return user;
    }

    /**
     * 更新用户并刷新缓存。
     * <p>
     * 使用 {@code @CachePut} 在更新后刷新缓存，保证缓存与数据库一致。
     * </p>
     */
    @CachePut(cacheNames = "users", key = "#id")
    public UserDTO updateUser(Long id, String username, String email) {
        UserDTO user = UserDTO.builder()
                .id(id)
                .username(username)
                .email(email)
                .status(1)
                .createTime(LocalDateTime.now())
                .build();
        log.info("[声明式] 更新用户并刷新缓存: id={}", id);
        return user;
    }

    /**
     * 删除用户并清除缓存。
     * <p>
     * {@code @CacheEvict} 在方法执行后删除对应 key 的缓存。
     * {@code allEntries = true} 可清空整个 cacheName 下所有条目。
     * </p>
     */
    @CacheEvict(cacheNames = "users", key = "#id")
    public boolean deleteUser(Long id) {
        log.info("[声明式] 删除用户并清除缓存: id={}", id);
        return true;
    }

    /**
     * 清空所有用户缓存。
     */
    @CacheEvict(cacheNames = "users", allEntries = true)
    public void clearAllUserCache() {
        log.info("[声明式] 清空 users 缓存所有条目");
    }

    // ==================== 条件缓存示例 ====================

    /**
     * 条件缓存：仅当 status=1（启用）时缓存结果。
     * <p>
     * {@code condition} 在方法执行前评估，决定是否走缓存逻辑。
     * {@code unless} 在方法执行后评估，决定是否将结果放入缓存。
     * </p>
     */
    @Cacheable(cacheNames = "activeUsers", key = "#id",
            condition = "#id != null && #id > 0",
            unless = "#result == null || #result.status != 1")
    public UserDTO getActiveUser(Long id) {
        log.info("[声明式-条件] 方法体执行: id={}", id);
        return mockDbQuery(id);
    }

    // ==================== 私有方法 ====================

    private UserDTO mockDbQuery(Long id) {
        if (id == null || id < 1) {
            return null;
        }
        // 模拟数据库延迟
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return UserDTO.builder()
                .id(id)
                .username("declarative_user" + id)
                .email("user" + id + "@example.com")
                .status(id % 2 == 0 ? 0 : 1)
                .createTime(LocalDateTime.now().minusHours(id))
                .build();
    }
}
