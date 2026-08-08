package cn.aryee.examples.cache.reactive.service;

import cn.aryee.cache.api.service.ReactiveCacheService;
import cn.aryee.examples.cache.reactive.model.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于响应式缓存的接口幂等性实现示例。
 * <p>
 * 使用 {@link ReactiveCacheService} 的原子递增操作实现幂等控制，
 * 全程非阻塞，适用于 WebFlux 高并发场景。
 * </p>
 *
 * <h2>响应式幂等实现要点</h2>
 * <ol>
 *   <li>使用 {@code reactiveCacheService.increment()} 的原子性保证并发安全</li>
 *   <li>通过 {@code flatMap} 链式操作：递增 → 判断是否首次 → 执行/返回缓存</li>
 *   <li>禁止使用 {@code block()} 获取结果，全部通过 Reactive 流传递</li>
 * </ol>
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReactiveIdempotentService {

    private final ReactiveCacheService reactiveCacheService;
    private final AtomicLong idGenerator = new AtomicLong(300L);

    private static final String IDEMPOTENT_KEY_PREFIX = "reactive:idempotent:request:";
    private static final String IDEMPOTENT_RESULT_PREFIX = "reactive:idempotent:result:";
    private static final Duration IDEMPOTENT_WINDOW = Duration.ofMinutes(10);

    /**
     * 幂等创建用户（非阻塞）。
     * <p>
     * 同一 {@code requestId} 在幂等窗口内多次调用，只执行一次业务逻辑。
     * 通过 {@code increment} 的原子性实现并发安全，无需分布式锁。
     * </p>
     *
     * @param requestId 请求唯一标识
     * @param username  用户名
     * @param email     邮箱
     * @return 创建结果（首次创建或重复请求返回的缓存结果）
     */
    public Mono<UserDTO> createUserIdempotent(String requestId, String username, String email) {
        if (requestId == null || requestId.isBlank()) {
            return Mono.error(new IllegalArgumentException("requestId 不能为空"));
        }

        String countKey = IDEMPOTENT_KEY_PREFIX + requestId;
        String resultKey = IDEMPOTENT_RESULT_PREFIX + requestId;

        // 原子递增计数器
        return reactiveCacheService.increment(countKey, 1)
                .flatMap(count -> {
                    if (count == 1) {
                        // 首次请求，设置窗口 TTL 并执行业务
                        log.info("[Reactive-幂等] 首次请求，执行业务逻辑: requestId={}", requestId);
                        UserDTO user = doCreateUser(username, email);
                        return reactiveCacheService.expire(countKey, IDEMPOTENT_WINDOW)
                                .then(reactiveCacheService.set(resultKey, user, IDEMPOTENT_WINDOW))
                                .thenReturn(user);
                    }

                    // 重复请求，返回缓存结果
                    log.info("[Reactive-幂等] 重复请求（第 {} 次），返回缓存结果: requestId={}", count, requestId);
                    return reactiveCacheService.<UserDTO>get(resultKey)
                            .switchIfEmpty(Mono.defer(() -> {
                                // 边界情况：结果缓存已过期但计数还在
                                log.warn("[Reactive-幂等] 结果缓存已过期，重新执行: requestId={}", requestId);
                                return Mono.just(doCreateUser(username, email));
                            }));
                });
    }

    /**
     * 检查 requestId 是否已处理。
     */
    public Mono<Boolean> isAlreadyProcessed(String requestId) {
        return reactiveCacheService.<Long>get(IDEMPOTENT_KEY_PREFIX + requestId)
                .map(count -> count != null && count > 0)
                .defaultIfEmpty(false);
    }

    /**
     * 清除幂等标记。
     */
    public Mono<Void> clearIdempotentMark(String requestId) {
        return reactiveCacheService.delete(IDEMPOTENT_KEY_PREFIX + requestId)
                .then(reactiveCacheService.delete(IDEMPOTENT_RESULT_PREFIX + requestId))
                .doOnSuccess(v -> log.info("[Reactive-幂等] 清除标记: requestId={}", requestId))
                .then();
    }

    private UserDTO doCreateUser(String username, String email) {
        return UserDTO.builder()
                .id(idGenerator.getAndIncrement())
                .username(username)
                .email(email)
                .status(1)
                .createTime(LocalDateTime.now())
                .build();
    }
}
