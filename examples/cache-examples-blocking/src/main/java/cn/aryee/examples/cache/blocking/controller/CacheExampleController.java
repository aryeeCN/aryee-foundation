package cn.aryee.examples.cache.blocking.controller;

import cn.aryee.examples.cache.blocking.model.UserDTO;
import cn.aryee.examples.cache.blocking.service.DeclarativeCacheService;
import cn.aryee.examples.cache.blocking.service.IdempotentService;
import cn.aryee.examples.cache.blocking.service.ProgrammaticCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 缓存示例 REST API 控制器。
 * <p>
 * 提供三个维度的缓存使用示例端点：
 * <ul>
 *   <li>{@code /api/cache/programmatic/**} — 编程式缓存（直接使用 CacheService）</li>
 *   <li>{@code /api/cache/declarative/**} — 声明式缓存（Spring @Cacheable 注解）</li>
 *   <li>{@code /api/cache/idempotent/**} — 幂等性示例（基于缓存计数器）</li>
 * </ul>
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheExampleController {

    private final ProgrammaticCacheService programmaticCacheService;
    private final DeclarativeCacheService declarativeCacheService;
    private final IdempotentService idempotentService;

    // ==================== 编程式缓存端点 ====================

    /**
     * 创建用户（编程式缓存）。
     */
    @PostMapping("/programmatic/users")
    public ResponseEntity<UserDTO> createProgrammaticUser(
            @RequestParam String username, @RequestParam String email) {
        return ResponseEntity.ok(programmaticCacheService.createUser(username, email));
    }

    /**
     * 查询用户（编程式缓存，含穿透防护）。
     */
    @GetMapping("/programmatic/users/{id}")
    public ResponseEntity<UserDTO> getProgrammaticUser(@PathVariable Long id) {
        return ResponseEntity.ok(programmaticCacheService.getUser(id));
    }

    /**
     * 删除用户缓存（编程式）。
     */
    @DeleteMapping("/programmatic/users/{id}")
    public ResponseEntity<Boolean> deleteProgrammaticUser(@PathVariable Long id) {
        return ResponseEntity.ok(programmaticCacheService.deleteUser(id));
    }

    /**
     * 批量查询用户（编程式 multiGet）。
     */
    @PostMapping("/programmatic/users/batch")
    public ResponseEntity<Map<String, UserDTO>> batchGetProgrammaticUsers(@RequestBody List<Long> ids) {
        return ResponseEntity.ok(programmaticCacheService.multiGet(ids));
    }

    /**
     * 访问计数器示例（限流场景）。
     */
    @PostMapping("/programmatic/access/{userId}")
    public ResponseEntity<Long> recordAccess(@PathVariable Long userId) {
        return ResponseEntity.ok(programmaticCacheService.recordAccess(userId));
    }

    /**
     * 查看缓存剩余 TTL。
     */
    @GetMapping("/programmatic/users/{id}/ttl")
    public ResponseEntity<String> getTtl(@PathVariable Long id) {
        Duration ttl = programmaticCacheService.getRemainingTtl(id);
        return ResponseEntity.ok(ttl == null ? "key 不存在" : ttl.toString());
    }

    // ==================== 声明式缓存端点 ====================

    /**
     * 查询用户（声明式 @Cacheable）。
     * <p>
     * 首次请求会执行方法体（模拟 DB 查询），后续相同 id 请求直接返回缓存。
     * </p>
     */
    @GetMapping("/declarative/users/{id}")
    public ResponseEntity<UserDTO> getDeclarativeUser(@PathVariable Long id) {
        return ResponseEntity.ok(declarativeCacheService.getUserById(id));
    }

    /**
     * 创建用户（声明式 @CachePut）。
     */
    @PostMapping("/declarative/users")
    public ResponseEntity<UserDTO> createDeclarativeUser(
            @RequestParam String username, @RequestParam String email) {
        return ResponseEntity.ok(declarativeCacheService.createUser(username, email));
    }

    /**
     * 更新用户（声明式 @CachePut 刷新缓存）。
     */
    @PutMapping("/declarative/users/{id}")
    public ResponseEntity<UserDTO> updateDeclarativeUser(
            @PathVariable Long id, @RequestParam String username, @RequestParam String email) {
        return ResponseEntity.ok(declarativeCacheService.updateUser(id, username, email));
    }

    /**
     * 删除用户（声明式 @CacheEvict）。
     */
    @DeleteMapping("/declarative/users/{id}")
    public ResponseEntity<Boolean> deleteDeclarativeUser(@PathVariable Long id) {
        return ResponseEntity.ok(declarativeCacheService.deleteUser(id));
    }

    /**
     * 清空所有用户缓存（声明式 @CacheEvict allEntries）。
     */
    @DeleteMapping("/declarative/users")
    public ResponseEntity<Void> clearAllDeclarativeCache() {
        declarativeCacheService.clearAllUserCache();
        return ResponseEntity.noContent().build();
    }

    /**
     * 条件缓存示例（仅启用用户缓存）。
     */
    @GetMapping("/declarative/users/{id}/active")
    public ResponseEntity<UserDTO> getActiveUser(@PathVariable Long id) {
        return ResponseEntity.ok(declarativeCacheService.getActiveUser(id));
    }

    // ==================== 幂等性端点 ====================

    /**
     * 幂等创建用户。
     * <p>
     * 客户端需在请求头中携带 {@code X-Request-Id}（UUID），
     * 同一 requestId 在 10 分钟窗口内重复请求只执行一次业务逻辑。
     * </p>
     *
     * @param requestId 请求唯一标识（请求头 X-Request-Id）
     * @param username  用户名
     * @param email     邮箱
     */
    @PostMapping("/idempotent/users")
    public ResponseEntity<UserDTO> createIdempotentUser(
            @RequestHeader("X-Request-Id") String requestId,
            @RequestParam String username,
            @RequestParam String email) {
        log.info("收到幂等创建请求: requestId={}, username={}", requestId, username);
        return ResponseEntity.ok(idempotentService.createUserIdempotent(requestId, username, email));
    }

    /**
     * 检查 requestId 是否已处理。
     */
    @GetMapping("/idempotent/check/{requestId}")
    public ResponseEntity<Boolean> checkIdempotent(@PathVariable String requestId) {
        return ResponseEntity.ok(idempotentService.isAlreadyProcessed(requestId));
    }

    /**
     * 清除幂等标记（调试用）。
     */
    @DeleteMapping("/idempotent/{requestId}")
    public ResponseEntity<Void> clearIdempotent(@PathVariable String requestId) {
        idempotentService.clearIdempotentMark(requestId);
        return ResponseEntity.noContent().build();
    }
}
