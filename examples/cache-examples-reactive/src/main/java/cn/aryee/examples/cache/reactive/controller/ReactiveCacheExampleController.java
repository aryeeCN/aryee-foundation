package cn.aryee.examples.cache.reactive.controller;

import cn.aryee.examples.cache.reactive.model.UserDTO;
import cn.aryee.examples.cache.reactive.service.ReactiveIdempotentService;
import cn.aryee.examples.cache.reactive.service.ReactiveProgrammaticCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 响应式缓存示例 REST API 控制器（WebFlux）。
 * <p>
 * 所有端点返回 {@link Mono}，全程非阻塞。
 * </p>
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class ReactiveCacheExampleController {

    private final ReactiveProgrammaticCacheService programmaticCacheService;
    private final ReactiveIdempotentService idempotentService;

    // ==================== 编程式缓存端点 ====================

    /**
     * 创建用户（响应式缓存）。
     */
    @PostMapping("/programmatic/users")
    public Mono<UserDTO> createProgrammaticUser(
            @RequestParam String username, @RequestParam String email) {
        return programmaticCacheService.createUser(username, email);
    }

    /**
     * 查询用户（响应式缓存，含穿透防护）。
     */
    @GetMapping("/programmatic/users/{id}")
    public Mono<UserDTO> getProgrammaticUser(@PathVariable Long id) {
        return programmaticCacheService.getUser(id);
    }

    /**
     * 删除用户缓存。
     */
    @DeleteMapping("/programmatic/users/{id}")
    public Mono<Boolean> deleteProgrammaticUser(@PathVariable Long id) {
        return programmaticCacheService.deleteUser(id);
    }

    /**
     * 批量查询用户。
     */
    @PostMapping("/programmatic/users/batch")
    public Mono<Map<String, UserDTO>> batchGetProgrammaticUsers(@RequestBody List<Long> ids) {
        return programmaticCacheService.multiGet(ids);
    }

    /**
     * 访问计数器示例。
     */
    @PostMapping("/programmatic/access/{userId}")
    public Mono<Long> recordAccess(@PathVariable Long userId) {
        return programmaticCacheService.recordAccess(userId);
    }

    /**
     * 查看缓存剩余 TTL。
     */
    @GetMapping("/programmatic/users/{id}/ttl")
    public Mono<String> getTtl(@PathVariable Long id) {
        return programmaticCacheService.getRemainingTtl(id)
                .map(ttl -> ttl == null ? "key 不存在" : ttl.toString())
                .defaultIfEmpty("key 不存在");
    }

    // ==================== 幂等性端点 ====================

    /**
     * 幂等创建用户（响应式）。
     * <p>
     * 客户端需在请求头中携带 {@code X-Request-Id}。
     * </p>
     */
    @PostMapping("/idempotent/users")
    public Mono<UserDTO> createIdempotentUser(
            @RequestHeader("X-Request-Id") String requestId,
            @RequestParam String username,
            @RequestParam String email) {
        log.info("收到 Reactive 幂等创建请求: requestId={}, username={}", requestId, username);
        return idempotentService.createUserIdempotent(requestId, username, email);
    }

    /**
     * 检查 requestId 是否已处理。
     */
    @GetMapping("/idempotent/check/{requestId}")
    public Mono<Boolean> checkIdempotent(@PathVariable String requestId) {
        return idempotentService.isAlreadyProcessed(requestId);
    }

    /**
     * 清除幂等标记。
     */
    @DeleteMapping("/idempotent/{requestId}")
    public Mono<Void> clearIdempotent(@PathVariable String requestId) {
        return idempotentService.clearIdempotentMark(requestId);
    }
}
