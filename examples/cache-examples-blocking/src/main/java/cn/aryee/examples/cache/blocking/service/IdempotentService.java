package cn.aryee.examples.cache.blocking.service;

import cn.aryee.cache.api.service.CacheService;
import cn.aryee.examples.cache.blocking.model.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于缓存的接口幂等性实现示例。
 * <p>
 * 幂等性（Idempotency）指同一请求执行一次与多次的效果完全一致。
 * 常见场景：支付接口、订单创建、消息消费等需要防重复处理的场景。
 * </p>
 *
 * <h2>实现原理</h2>
 * <ol>
 *   <li>客户端在请求头中携带唯一 {@code requestId}（UUID/雪花 ID）</li>
 *   <li>服务端处理前，使用 {@code CacheService.increment} 原子递增该 requestId 的计数</li>
 *   <li>若计数为 1（首次请求）→ 执行业务逻辑，缓存结果</li>
 *   <li>若计数 > 1（重复请求）→ 直接返回缓存的首次结果</li>
 *   <li>设置 TTL（如 10 分钟）自动清理过期 requestId</li>
 * </ol>
 *
 * <h2>对比其他幂等方案</h2>
 * <ul>
 *   <li><b>数据库唯一索引</b>：强一致，但性能差，适合写入场景</li>
 *   <li><b>分布式锁</b>：防并发，但不天然幂等（锁释放后仍可重复）</li>
 *   <li><b>缓存计数器（本方案）</b>：高性能，适合读多写少场景，TTL 内保证幂等</li>
 *   <li><b>状态机</b>：业务复杂时使用，如订单状态流转</li>
 * </ul>
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotentService {

    private final CacheService cacheService;

    /** 幂等 key 前缀 */
    private static final String IDEMPOTENT_KEY_PREFIX = "idempotent:request:";

    /** 幂等结果 key 前缀（存储首次执行结果） */
    private static final String IDEMPOTENT_RESULT_PREFIX = "idempotent:result:";

    /** 幂等窗口期（默认 10 分钟） */
    private static final Duration IDEMPOTENT_WINDOW = Duration.ofMinutes(10);

    /** 模拟数据库 ID 生成 */
    private final AtomicLong idGenerator = new AtomicLong(200L);

    /** 模拟数据库存储（演示用） */
    private final Map<Long, UserDTO> mockDb = new ConcurrentHashMap<>();

    /**
     * 幂等创建用户。
     * <p>
     * 同一 {@code requestId} 在幂等窗口内多次调用，只执行一次业务逻辑，
     * 后续重复请求直接返回首次结果。
     * </p>
     *
     * @param requestId 请求唯一标识（由客户端生成，如 UUID）
     * @param username  用户名
     * @param email     邮箱
     * @return 创建结果（首次创建或重复请求返回的缓存结果）
     */
    public UserDTO createUserIdempotent(String requestId, String username, String email) {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId 不能为空");
        }

        String countKey = IDEMPOTENT_KEY_PREFIX + requestId;
        String resultKey = IDEMPOTENT_RESULT_PREFIX + requestId;

        // 原子递增计数器（利用 Redis INCR 的原子性）
        long count = cacheService.increment(countKey, 1);
        if (count == 1) {
            // 首次请求，设置窗口 TTL
            cacheService.expire(countKey, IDEMPOTENT_WINDOW);

            // 执行业务逻辑
            log.info("[幂等] 首次请求，执行业务逻辑: requestId={}", requestId);
            UserDTO user = doCreateUser(username, email);

            // 缓存结果
            cacheService.set(resultKey, user, IDEMPOTENT_WINDOW);
            return user;
        }

        // 重复请求，返回缓存结果
        log.info("[幂等] 重复请求（第 {} 次），返回缓存结果: requestId={}", count, requestId);
        UserDTO cached = cacheService.get(resultKey);
        if (cached == null) {
            // 结果缓存已过期但计数还在（边界情况），重新执行
            log.warn("[幂等] 结果缓存已过期，重新执行: requestId={}", requestId);
            return doCreateUser(username, email);
        }
        return cached;
    }

    /**
     * 检查 requestId 是否已处理过（不执行业务逻辑）。
     */
    public boolean isAlreadyProcessed(String requestId) {
        String countKey = IDEMPOTENT_KEY_PREFIX + requestId;
        Long count = cacheService.get(countKey);
        return count != null && count > 0;
    }

    /**
     * 手动清除幂等标记（用于调试/重置）。
     */
    public void clearIdempotentMark(String requestId) {
        cacheService.delete(IDEMPOTENT_KEY_PREFIX + requestId);
        cacheService.delete(IDEMPOTENT_RESULT_PREFIX + requestId);
        log.info("[幂等] 清除标记: requestId={}", requestId);
    }

    // ==================== 私有方法 ====================

    private UserDTO doCreateUser(String username, String email) {
        UserDTO user = UserDTO.builder()
                .id(idGenerator.getAndIncrement())
                .username(username)
                .email(email)
                .status(1)
                .createTime(java.time.LocalDateTime.now())
                .build();
        mockDb.put(user.getId(), user);
        return user;
    }
}
