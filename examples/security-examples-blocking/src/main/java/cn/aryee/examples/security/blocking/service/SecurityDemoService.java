package cn.aryee.examples.security.blocking.service;

import cn.aryee.security.api.enums.AccountStatus;
import cn.aryee.security.api.model.AuthRequest;
import cn.aryee.security.api.model.AuthResponse;
import cn.aryee.security.api.model.UserPrincipal;
import cn.aryee.security.api.service.AccountManagementService;
import cn.aryee.security.api.service.EnterpriseAuthService;
import cn.aryee.security.api.service.PasswordPolicyService;
import cn.aryee.security.api.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Security Blocking 示例服务
 * 演示如何在业务层使用 Security 模块的企业级安全功能
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityDemoService {

    private final TokenService tokenService;
    private final EnterpriseAuthService enterpriseAuthService;
    private final AccountManagementService accountManagementService;
    private final PasswordPolicyService passwordPolicyService;

    // ========== Token 管理 ==========

    /**
     * 生成 Token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @return 包含 token 的响应
     */
    public Map<String, Object> generateToken(String userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);

        String token = tokenService.generateToken(userId, claims);
        String refreshToken = tokenService.refreshToken(token);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("refreshToken", refreshToken);
        result.put("userId", userId);
        result.put("username", username);
        return result;
    }

    /**
     * 验证 Token
     *
     * @param token 令牌
     * @return 验证结果
     */
    public Map<String, Object> validateToken(String token) {
        boolean valid = tokenService.validateToken(token);
        Map<String, Object> result = new HashMap<>();
        result.put("valid", valid);

        if (valid) {
            Map<String, Object> claims = tokenService.parseToken(token);
            result.put("claims", claims);
            result.put("userId", tokenService.getUserIdFromToken(token));
        }
        return result;
    }

    /**
     * 使 Token 失效
     *
     * @param token 令牌
     * @return 操作结果
     */
    public Map<String, Object> invalidateToken(String token) {
        tokenService.invalidateToken(token);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Token 已失效");
        return result;
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
    public AuthResponse login(String username, String password, String clientIp) {
        AuthRequest request = new AuthRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setClientIp(clientIp);
        request.setDeviceId("demo-device-001");
        request.setDeviceType("web");
        return enterpriseAuthService.enterpriseLogin(request);
    }

    /**
     * 刷新令牌
     *
     * @param refreshToken 刷新令牌
     * @return 新的认证响应
     */
    public AuthResponse refreshToken(String refreshToken) {
        return enterpriseAuthService.refreshToken(refreshToken, "demo-device-001");
    }

    // ========== 账户管理 ==========

    /**
     * 获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    public Map<String, Object> getUserInfo(String userId) {
        Map<String, Object> result = new HashMap<>();
        accountManagementService.getUserById(userId).ifPresentOrElse(
                user -> {
                    result.put("found", true);
                    result.put("userId", user.getUserId());
                    result.put("username", user.getUsername());
                    result.put("email", user.getEmail());
                    result.put("accountStatus", user.getAccountStatus());
                    result.put("enabled", user.getEnabled());
                    result.put("roles", user.getRoles());
                    result.put("permissions", user.getPermissions());
                    result.put("lastLoginTime", user.getLastLoginTime());
                    result.put("lastLoginIp", user.getLastLoginIp());
                    result.put("loginCount", user.getLoginCount());
                },
                () -> result.put("found", false)
        );
        return result;
    }

    /**
     * 获取账户状态
     *
     * @param userId 用户ID
     * @return 账户状态信息
     */
    public Map<String, Object> getAccountStatus(String userId) {
        AccountStatus status = accountManagementService.getAccountStatus(userId);
        boolean canLogin = accountManagementService.canLogin(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("status", status);
        result.put("canLogin", canLogin);
        return result;
    }

    /**
     * 锁定/解锁账户
     *
     * @param userId   用户ID
     * @param lock     true=锁定, false=解锁
     * @param reason   操作原因
     * @param operator 操作者ID
     * @return 操作结果
     */
    public Map<String, Object> toggleAccountLock(String userId, boolean lock, String reason, String operator) {
        boolean success;
        if (lock) {
            success = enterpriseAuthService.lockAccount(userId, reason, operator);
        } else {
            success = enterpriseAuthService.unlockAccount(userId, reason, operator);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("action", lock ? "lock" : "unlock");
        result.put("userId", userId);
        result.put("currentStatus", accountManagementService.getAccountStatus(userId));
        return result;
    }

    // ========== 密码策略 ==========

    /**
     * 评估密码强度
     *
     * @param password 密码
     * @return 评估结果
     */
    public Map<String, Object> evaluatePassword(String password) {
        int score = passwordPolicyService.evaluatePasswordStrength(password);
        List<String> errors = passwordPolicyService.validatePassword(password);

        String level;
        if (score >= 85) {
            level = "非常强";
        } else if (score >= 75) {
            level = "强";
        } else if (score >= 50) {
            level = "中等";
        } else {
            level = "弱";
        }

        Map<String, Object> result = new HashMap<>();
        result.put("score", score);
        result.put("level", level);
        result.put("errors", errors);
        result.put("passed", errors.isEmpty());
        return result;
    }

    /**
     * 生成临时密码
     *
     * @return 临时密码信息
     */
    public Map<String, Object> generateTemporaryPassword() {
        String tempPassword = passwordPolicyService.generateTemporaryPassword();
        String randomPassword = passwordPolicyService.generateRandomPassword(16);

        Map<String, Object> result = new HashMap<>();
        result.put("temporaryPassword", tempPassword);
        result.put("randomPassword", randomPassword);
        result.put("tempLength", tempPassword.length());
        result.put("randomLength", randomPassword.length());
        return result;
    }

    // ========== 会话管理 ==========

    /**
     * 获取用户会话信息
     *
     * @param userId 用户ID
     * @return 会话信息
     */
    public Map<String, Object> getSessionInfo(String userId) {
        int activeCount = enterpriseAuthService.getActiveSessionCount(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("activeSessionCount", activeCount);
        return result;
    }

    /**
     * 强制登出用户所有会话
     *
     * @param userId   用户ID
     * @param reason   原因
     * @param operator 操作者
     * @return 操作结果
     */
    public Map<String, Object> forceLogoutAll(String userId, String reason, String operator) {
        int count = enterpriseAuthService.forceLogoutAll(userId, reason, operator);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("loggedOutCount", count);
        result.put("userId", userId);
        return result;
    }

    /**
     * 查询登录日志
     *
     * @param userId   用户ID
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 登录日志列表
     */
    public Map<String, Object> getLoginLogs(String userId, int pageNum, int pageSize) {
        List<EnterpriseAuthService.LoginLog> logs = enterpriseAuthService.getLoginLogs(userId, pageNum, pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("total", logs.size());
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        result.put("logs", logs);
        return result;
    }
}
