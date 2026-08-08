package cn.aryee.examples.security.reactive.service;

import cn.aryee.security.api.model.AuthRequest;
import cn.aryee.security.api.model.AuthResponse;
import cn.aryee.security.api.model.UserPrincipal;
import cn.aryee.security.api.service.ReactiveAuthService;
import cn.aryee.security.api.service.ReactiveCryptoService;
import cn.aryee.security.api.service.ReactiveTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Security Reactive 示例服务
 * 演示如何在响应式业务层使用 Security 模块的安全功能
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityReactiveDemoService {

    private final ReactiveTokenService reactiveTokenService;
    private final ReactiveAuthService reactiveAuthService;
    private final ReactiveCryptoService reactiveCryptoService;

    // ========== Token 管理 ==========

    /**
     * 生成 Token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @return 包含 token 的响应
     */
    public Mono<Map<String, Object>> generateToken(String userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);

        return reactiveTokenService.generateToken(userId, claims)
                .flatMap(token -> reactiveTokenService.refreshToken(token)
                        .map(refreshToken -> {
                            Map<String, Object> result = new HashMap<>();
                            result.put("token", token);
                            result.put("refreshToken", refreshToken);
                            result.put("userId", userId);
                            result.put("username", username);
                            return result;
                        }));
    }

    /**
     * 验证 Token
     *
     * @param token 令牌
     * @return 验证结果
     */
    public Mono<Map<String, Object>> validateToken(String token) {
        return reactiveTokenService.validateToken(token)
                .flatMap(valid -> {
                    if (valid) {
                        return reactiveTokenService.parseToken(token)
                                .map(claims -> {
                                    Map<String, Object> result = new HashMap<>();
                                    result.put("valid", true);
                                    result.put("claims", claims);
                                    return result;
                                })
                                .zipWith(reactiveTokenService.getUserIdFromToken(token))
                                .map(tuple -> {
                                    Map<String, Object> result = tuple.getT1();
                                    result.put("userId", tuple.getT2());
                                    return result;
                                });
                    } else {
                        Map<String, Object> result = new HashMap<>();
                        result.put("valid", false);
                        return Mono.just(result);
                    }
                });
    }

    /**
     * 使 Token 失效
     *
     * @param token 令牌
     * @return 操作结果
     */
    public Mono<Map<String, Object>> invalidateToken(String token) {
        return reactiveTokenService.invalidateToken(token)
                .thenReturn(Map.of("success", true, "message", "Token 已失效"));
    }

    // ========== 认证管理 ==========

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @param clientIp 客户端IP
     * @return 登录响应
     */
    public Mono<AuthResponse> login(String username, String password, String clientIp) {
        AuthRequest request = new AuthRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setClientIp(clientIp);
        request.setDeviceId("reactive-demo-device-001");
        request.setDeviceType("web");
        return reactiveAuthService.login(request);
    }

    /**
     * 用户登出
     *
     * @param token 访问令牌
     * @return 操作结果
     */
    public Mono<Map<String, Object>> logout(String token) {
        return reactiveAuthService.logout(token)
                .thenReturn(Map.of("success", true, "message", "已登出"));
    }

    /**
     * 验证 Token
     *
     * @param token 访问令牌
     * @return 验证结果
     */
    public Mono<Map<String, Object>> verifyToken(String token) {
        return reactiveAuthService.verifyToken(token)
                .map(valid -> Map.of("valid", valid));
    }

    /**
     * 刷新令牌
     *
     * @param refreshToken 刷新令牌
     * @return 新的认证响应
     */
    public Mono<AuthResponse> refreshToken(String refreshToken) {
        return reactiveAuthService.refreshAuthToken(refreshToken);
    }

    /**
     * 获取用户信息
     *
     * @param token 访问令牌
     * @return 用户信息
     */
    public Mono<Map<String, Object>> getUserInfo(String token) {
        return reactiveAuthService.getUserInfo(token)
                .map(user -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("userId", user.getUserId());
                    result.put("username", user.getUsername());
                    result.put("email", user.getEmail());
                    result.put("enabled", user.getEnabled());
                    result.put("roles", user.getRoles());
                    result.put("permissions", user.getPermissions());
                    result.put("accountStatus", user.getAccountStatus());
                    result.put("lastLoginTime", user.getLastLoginTime());
                    return result;
                });
    }

    /**
     * 检查权限
     *
     * @param token      访问令牌
     * @param permission 权限标识
     * @return 是否拥有权限
     */
    public Mono<Map<String, Object>> hasPermission(String token, String permission) {
        return reactiveAuthService.hasPermission(token, permission)
                .map(has -> Map.of("hasPermission", has, "permission", permission));
    }

    /**
     * 检查角色
     *
     * @param token 访问令牌
     * @param role  角色标识
     * @return 是否拥有角色
     */
    public Mono<Map<String, Object>> hasRole(String token, String role) {
        return reactiveAuthService.hasRole(token, role)
                .map(has -> Map.of("hasRole", has, "role", role));
    }

    // ========== 加密服务 ==========

    /**
     * 密码加密
     *
     * @param rawPassword 原始密码
     * @return 加密后的密码
     */
    public Mono<Map<String, Object>> encryptPassword(String rawPassword) {
        return reactiveCryptoService.encryptPassword(rawPassword)
                .map(encoded -> Map.of("rawPassword", rawPassword, "encodedPassword", encoded));
    }

    /**
     * 验证密码
     *
     * @param rawPassword    原始密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    public Mono<Map<String, Object>> verifyPassword(String rawPassword, String encodedPassword) {
        return reactiveCryptoService.matchesPassword(rawPassword, encodedPassword)
                .map(matches -> Map.of("matches", matches));
    }

    /**
     * 计算哈希
     *
     * @param input 输入字符串
     * @return 哈希结果
     */
    public Mono<Map<String, Object>> hash(String input) {
        return reactiveCryptoService.md5(input)
                .zipWith(reactiveCryptoService.sha256(input))
                .map(tuple -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("input", input);
                    result.put("md5", tuple.getT1());
                    result.put("sha256", tuple.getT2());
                    return result;
                });
    }

    /**
     * AES 加密/解密演示
     *
     * @param data 明文数据
     * @return 加解密结果
     */
    public Mono<Map<String, Object>> aesDemo(String data) {
        return reactiveCryptoService.generateAesKey()
                .flatMap(key -> reactiveCryptoService.aesEncrypt(data, key)
                        .flatMap(encrypted -> reactiveCryptoService.aesDecrypt(encrypted, key)
                                .map(decrypted -> {
                                    Map<String, Object> result = new HashMap<>();
                                    result.put("original", data);
                                    result.put("aesKey", key);
                                    result.put("encrypted", encrypted);
                                    result.put("decrypted", decrypted);
                                    return result;
                                })));
    }
}
