package cn.aryee.examples.cache.blocking.service;

import cn.aryee.cache.api.service.CacheService;
import cn.aryee.examples.cache.blocking.model.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 编程式缓存使用示例。
 * <p>
 * 直接注入 {@link CacheService} 进行手动缓存读写，适用于需要精细控制缓存 key、
 * TTL、批量操作等场景。展示了以下能力：
 * <ul>
 *   <li>基本 get/set/delete</li>
 *   <li>TTL 过期设置</li>
 *   <li>批量 multiGet / multiSet</li>
 *   <li>计数器 increment（可用于限流、幂等计数）</li>
 *   <li>Hash 数据结构</li>
 * </ul>
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProgrammaticCacheService {

    private final CacheService cacheService;

    /** 模拟数据库自增 ID */
    private final AtomicLong idGenerator = new AtomicLong(1L);

    /** 缓存 key 前缀（业务维度隔离） */
    private static final String KEY_PREFIX = "prog:user:";
    private static final String HASH_KEY = "prog:user:hash";

    // ==================== 基本 String 缓存 ====================

    /**
     * 创建用户并缓存。
     * 演示 {@link CacheService#set(String, Object, Duration)} 设置带 TTL 的缓存。
     */
    public UserDTO createUser(String username, String email) {
        UserDTO user = UserDTO.builder()
                .id(idGenerator.getAndIncrement())
                .username(username)
                .email(email)
                .status(1)
                .createTime(LocalDateTime.now())
                .build();

        // 缓存 10 分钟
        cacheService.set(KEY_PREFIX + user.getId(), user, Duration.ofMinutes(10));
        log.info("[编程式] 创建用户并缓存: id={}, username={}", user.getId(), username);
        return user;
    }

    /**
     * 查询用户（先查缓存，未命中则回源）。
     * 演示 {@link CacheService#get(String)} 和缓存穿透防护。
     */
    public UserDTO getUser(Long id) {
        String key = KEY_PREFIX + id;
        UserDTO user = cacheService.get(key);
        if (user != null) {
            log.info("[编程式] 缓存命中: id={}", id);
            return user;
        }

        // 缓存未命中，模拟回源查数据库
        log.info("[编程式] 缓存未命中，回源查询: id={}", id);
        user = mockDbQuery(id);
        if (user != null) {
            // 回填缓存，设置较短 TTL 防止脏数据
            cacheService.set(key, user, Duration.ofMinutes(5));
        } else {
            // 缓存空值防止缓存穿透（设置短 TTL）
            cacheService.set(key, "NULL", Duration.ofSeconds(30));
            log.info("[编程式] 写入空值标记防止穿透: id={}", id);
        }
        return user;
    }

    /**
     * 删除用户缓存。
     */
    public boolean deleteUser(Long id) {
        boolean deleted = cacheService.delete(KEY_PREFIX + id);
        log.info("[编程式] 删除缓存: id={}, result={}", id, deleted);
        return deleted;
    }

    /**
     * 检查 key 是否存在。
     */
    public boolean exists(Long id) {
        return cacheService.hasKey(KEY_PREFIX + id);
    }

    // ==================== TTL 管理 ====================

    /**
     * 获取缓存剩余过期时间。
     */
    public Duration getRemainingTtl(Long id) {
        return cacheService.getExpire(KEY_PREFIX + id);
    }

    /**
     * 续期缓存（延长 TTL）。
     */
    public boolean renewTtl(Long id, Duration duration) {
        return cacheService.expire(KEY_PREFIX + id, duration);
    }

    // ==================== 批量操作 ====================

    /**
     * 批量查询用户。
     * 演示 {@link CacheService#multiGet(List)}。
     */
    public Map<String, UserDTO> multiGet(List<Long> ids) {
        List<String> keys = ids.stream().map(id -> KEY_PREFIX + id).toList();
        Map<String, UserDTO> result = cacheService.multiGet(keys);
        log.info("[编程式] 批量查询: 请求 {} 个, 命中 {} 个", ids.size(), result.size());
        return result;
    }

    /**
     * 批量设置缓存。
     * 演示 {@link CacheService#multiSet(Map)}。
     */
    public void multiSet(Map<Long, UserDTO> users) {
        Map<String, Object> cacheMap = new HashMap<>();
        users.forEach((id, user) -> cacheMap.put(KEY_PREFIX + id, user));
        cacheService.multiSet(cacheMap);
        log.info("[编程式] 批量设置缓存: {} 条", users.size());
    }

    // ==================== 计数器（限流/幂等计数） ====================

    /**
     * 计数器示例：记录某用户在时间窗口内的访问次数。
     * 演示 {@link CacheService#increment(String, long)}。
     */
    public long recordAccess(Long userId) {
        String countKey = "prog:access:count:" + userId;
        long count = cacheService.increment(countKey, 1);
        if (count == 1) {
            // 首次访问，设置 1 分钟窗口
            cacheService.expire(countKey, Duration.ofMinutes(1));
        }
        log.info("[编程式] 用户 {} 访问计数: {}（1分钟窗口）", userId, count);
        return count;
    }

    // ==================== Hash 数据结构 ====================

    /**
     * 将用户信息存入 Hash 结构（适合存储对象的多个字段）。
     * 演示 {@link CacheService#hSet(String, String, Object)}。
     */
    public void cacheUserToHash(UserDTO user) {
        String field = String.valueOf(user.getId());
        cacheService.hSet(HASH_KEY, field, user);
        log.info("[编程式] Hash 缓存用户: field={}", field);
    }

    /**
     * 从 Hash 结构获取用户。
     */
    public UserDTO getUserFromHash(Long id) {
        return cacheService.hGet(HASH_KEY, String.valueOf(id));
    }

    // ==================== 私有方法 ====================

    /**
     * 模拟数据库查询（仅 id=1~3 有数据）。
     */
    private UserDTO mockDbQuery(Long id) {
        if (id == null || id < 1 || id > 3) {
            return null;
        }
        return UserDTO.builder()
                .id(id)
                .username("user" + id)
                .email("user" + id + "@example.com")
                .status(1)
                .createTime(LocalDateTime.now().minusDays(id))
                .build();
    }
}
