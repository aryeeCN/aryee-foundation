package cn.aryee.examples.security.reactive.controller;

import cn.aryee.examples.security.reactive.service.SecurityReactiveDemoService;
import cn.aryee.security.api.model.AuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Security Reactive 示例 Controller
 * 演示响应式企业级安全功能的 REST API 端点
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
public class SecurityReactiveDemoController {

    private final SecurityReactiveDemoService securityReactiveDemoService;

    // ==================== Token 管理 ====================

    /**
     * 生成 Token
     */
    @PostMapping("/token/generate")
    public Mono<Map<String, Object>> generateToken(@RequestParam String userId,
                                                   @RequestParam String username) {
        log.info("生成 Token: userId={}, username={}", userId, username);
        return securityReactiveDemoService.generateToken(userId, username);
    }

    /**
     * 验证 Token
     */
    @GetMapping("/token/validate")
    public Mono<Map<String, Object>> validateToken(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return securityReactiveDemoService.validateToken(token);
    }

    /**
     * 使 Token 失效
     */
    @DeleteMapping("/token/invalidate")
    public Mono<Map<String, Object>> invalidateToken(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return securityReactiveDemoService.invalidateToken(token);
    }

    // ==================== 认证管理 ====================

    /**
     * 用户登录
     */
    @PostMapping("/auth/login")
    public Mono<AuthResponse> login(@RequestParam String username,
                                    @RequestParam String password,
                                    @RequestParam(defaultValue = "127.0.0.1") String clientIp) {
        log.info("用户登录: username={}", username);
        return securityReactiveDemoService.login(username, password, clientIp);
    }

    /**
     * 用户登出
     */
    @PostMapping("/auth/logout")
    public Mono<Map<String, Object>> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return securityReactiveDemoService.logout(token);
    }

    /**
     * 验证 Token
     */
    @GetMapping("/auth/verify")
    public Mono<Map<String, Object>> verifyToken(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return securityReactiveDemoService.verifyToken(token);
    }

    /**
     * 刷新令牌
     */
    @PostMapping("/auth/refresh")
    public Mono<AuthResponse> refreshToken(@RequestParam String refreshToken) {
        log.info("刷新令牌");
        return securityReactiveDemoService.refreshToken(refreshToken);
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/auth/user-info")
    public Mono<Map<String, Object>> getUserInfo(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return securityReactiveDemoService.getUserInfo(token);
    }

    // ==================== 权限管理 ====================

    /**
     * 检查权限
     */
    @GetMapping("/permission/check")
    public Mono<Map<String, Object>> hasPermission(@RequestHeader("Authorization") String authHeader,
                                                    @RequestParam String permission) {
        String token = authHeader.replace("Bearer ", "");
        return securityReactiveDemoService.hasPermission(token, permission);
    }

    /**
     * 检查角色
     */
    @GetMapping("/role/check")
    public Mono<Map<String, Object>> hasRole(@RequestHeader("Authorization") String authHeader,
                                              @RequestParam String role) {
        String token = authHeader.replace("Bearer ", "");
        return securityReactiveDemoService.hasRole(token, role);
    }

    // ==================== 加密服务 ====================

    /**
     * 密码加密
     */
    @PostMapping("/crypto/encrypt-password")
    public Mono<Map<String, Object>> encryptPassword(@RequestParam String password) {
        return securityReactiveDemoService.encryptPassword(password);
    }

    /**
     * 验证密码
     */
    @PostMapping("/crypto/verify-password")
    public Mono<Map<String, Object>> verifyPassword(@RequestParam String rawPassword,
                                                    @RequestParam String encodedPassword) {
        return securityReactiveDemoService.verifyPassword(rawPassword, encodedPassword);
    }

    /**
     * 计算哈希（MD5 + SHA-256）
     */
    @PostMapping("/crypto/hash")
    public Mono<Map<String, Object>> hash(@RequestParam String input) {
        return securityReactiveDemoService.hash(input);
    }

    /**
     * AES 加解密演示
     */
    @PostMapping("/crypto/aes-demo")
    public Mono<Map<String, Object>> aesDemo(@RequestParam String data) {
        return securityReactiveDemoService.aesDemo(data);
    }
}
