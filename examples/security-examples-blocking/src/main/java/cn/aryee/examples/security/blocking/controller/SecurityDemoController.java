package cn.aryee.examples.security.blocking.controller;

import cn.aryee.examples.security.blocking.service.SecurityDemoService;
import cn.aryee.security.api.model.AuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Security Blocking 示例 Controller
 * 演示企业级安全功能的 REST API 端点
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
public class SecurityDemoController {

    private final SecurityDemoService securityDemoService;

    // ==================== Token 管理 ====================

    /**
     * 生成 Token
     */
    @PostMapping("/token/generate")
    public Map<String, Object> generateToken(@RequestParam String userId,
                                             @RequestParam String username) {
        log.info("生成 Token: userId={}, username={}", userId, username);
        return securityDemoService.generateToken(userId, username);
    }

    /**
     * 验证 Token
     */
    @GetMapping("/token/validate")
    public Map<String, Object> validateToken(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return securityDemoService.validateToken(token);
    }

    /**
     * 使 Token 失效
     */
    @DeleteMapping("/token/invalidate")
    public Map<String, Object> invalidateToken(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return securityDemoService.invalidateToken(token);
    }

    // ==================== 认证管理 ====================

    /**
     * 用户登录
     */
    @PostMapping("/auth/login")
    public AuthResponse login(@RequestParam String username,
                              @RequestParam String password,
                              @RequestParam(defaultValue = "127.0.0.1") String clientIp) {
        log.info("用户登录: username={}", username);
        return securityDemoService.login(username, password, clientIp);
    }

    /**
     * 刷新令牌
     */
    @PostMapping("/auth/refresh")
    public AuthResponse refreshToken(@RequestParam String refreshToken) {
        log.info("刷新令牌");
        return securityDemoService.refreshToken(refreshToken);
    }

    // ==================== 账户管理 ====================

    /**
     * 获取用户信息
     */
    @GetMapping("/account/{userId}")
    public Map<String, Object> getUserInfo(@PathVariable String userId) {
        return securityDemoService.getUserInfo(userId);
    }

    /**
     * 获取账户状态
     */
    @GetMapping("/account/{userId}/status")
    public Map<String, Object> getAccountStatus(@PathVariable String userId) {
        return securityDemoService.getAccountStatus(userId);
    }

    /**
     * 锁定账户
     */
    @PostMapping("/account/{userId}/lock")
    public Map<String, Object> lockAccount(@PathVariable String userId,
                                           @RequestParam(defaultValue = "管理员操作") String reason,
                                           @RequestParam(defaultValue = "admin") String operator) {
        log.info("锁定账户: userId={}, reason={}", userId, reason);
        return securityDemoService.toggleAccountLock(userId, true, reason, operator);
    }

    /**
     * 解锁账户
     */
    @PostMapping("/account/{userId}/unlock")
    public Map<String, Object> unlockAccount(@PathVariable String userId,
                                             @RequestParam(defaultValue = "管理员操作") String reason,
                                             @RequestParam(defaultValue = "admin") String operator) {
        log.info("解锁账户: userId={}", userId);
        return securityDemoService.toggleAccountLock(userId, false, reason, operator);
    }

    // ==================== 密码策略 ====================

    /**
     * 评估密码强度
     */
    @PostMapping("/password/evaluate")
    public Map<String, Object> evaluatePassword(@RequestParam String password) {
        return securityDemoService.evaluatePassword(password);
    }

    /**
     * 生成临时密码
     */
    @GetMapping("/password/generate")
    public Map<String, Object> generatePassword() {
        return securityDemoService.generateTemporaryPassword();
    }

    // ==================== 会话管理 ====================

    /**
     * 获取用户会话信息
     */
    @GetMapping("/session/{userId}")
    public Map<String, Object> getSessionInfo(@PathVariable String userId) {
        return securityDemoService.getSessionInfo(userId);
    }

    /**
     * 强制登出所有会话
     */
    @PostMapping("/session/{userId}/force-logout")
    public Map<String, Object> forceLogoutAll(@PathVariable String userId,
                                              @RequestParam(defaultValue = "管理员操作") String reason,
                                              @RequestParam(defaultValue = "admin") String operator) {
        log.info("强制登出: userId={}, reason={}", userId, reason);
        return securityDemoService.forceLogoutAll(userId, reason, operator);
    }

    // ==================== 审计日志 ====================

    /**
     * 查询登录日志
     */
    @GetMapping("/audit/login-logs/{userId}")
    public Map<String, Object> getLoginLogs(@PathVariable String userId,
                                            @RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize) {
        return securityDemoService.getLoginLogs(userId, pageNum, pageSize);
    }
}
