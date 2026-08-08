package cn.aryee.examples.security.reactive;

import cn.aryee.security.api.config.*;
import cn.aryee.security.api.model.AuthRequest;
import cn.aryee.security.api.model.AuthResponse;
import cn.aryee.security.api.service.*;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security Reactive 集成测试
 * 测试响应式安全功能：Token管理、认证、加密
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootTest
@DisplayName("Security 响应式集成测试")
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
public class SecurityReactiveIntegrationTest {

    private final SecurityProperties securityProperties;

    private final ReactiveTokenService reactiveTokenService;

    private final ReactiveAuthService reactiveAuthService;

    private final ReactiveCryptoService reactiveCryptoService;

    @Test
    @DisplayName("Reactive Security 配置加载测试")
    void testSecurityPropertiesLoaded() {
        assertNotNull(securityProperties, "SecurityProperties 应该被加载");
        assertTrue(securityProperties.isEnabled(), "Security 模块应该启用");

        SecurityProperties.Jwt jwtConfig = securityProperties.getJwt();
        assertNotNull(jwtConfig, "JwtConfig 不应该为 null");
        assertEquals(3600, jwtConfig.getAccessTokenExpiration(), "AccessToken 过期时间应该是 3600 秒");

        PasswordPolicy passwordPolicy = securityProperties.getPasswordPolicy();
        assertNotNull(passwordPolicy, "PasswordPolicy 不应该为 null");
        assertEquals(8, passwordPolicy.getMinLength(), "最小密码长度应该是 8");
    }

    @Test
    @DisplayName("Reactive Token 生成和验证测试")
    void testReactiveTokenGeneration() {
        String userId = "1";
        java.util.Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("userId", userId);
        claims.put("username", "admin");

        // 生成 Token
        StepVerifier.create(reactiveTokenService.generateToken(userId, claims))
                .assertNext(token -> {
                    assertNotNull(token);
                    assertFalse(token.isEmpty());
                })
                .verifyComplete();

        // 验证 Token
        reactiveTokenService.generateToken(userId, claims)
                .flatMap(reactiveTokenService::validateToken)
                .as(StepVerifier::create)
                .assertNext(valid -> assertTrue(valid, "Token 应该有效"))
                .verifyComplete();

        // 解析 Token
        reactiveTokenService.generateToken(userId, claims)
                .flatMap(reactiveTokenService::parseToken)
                .as(StepVerifier::create)
                .assertNext(parsed -> {
                    assertEquals(userId, parsed.get("userId"));
                    assertEquals("admin", parsed.get("username"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Reactive Token 失效测试")
    void testReactiveTokenInvalidation() {
        String userId = "1";
        java.util.Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("userId", userId);

        reactiveTokenService.generateToken(userId, claims)
                .flatMap(reactiveTokenService::invalidateToken)
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    @DisplayName("Reactive 密码加密和验证测试")
    void testReactivePasswordEncryption() {
        String rawPassword = "TestPass123!";

        // 加密密码
        reactiveCryptoService.encryptPassword(rawPassword)
                .flatMap(encoded -> reactiveCryptoService.matchesPassword(rawPassword, encoded))
                .as(StepVerifier::create)
                .assertNext(matches -> assertTrue(matches, "密码应该匹配"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Reactive 哈希计算测试")
    void testReactiveHash() {
        String input = "hello-world";

        // MD5
        reactiveCryptoService.md5(input)
                .as(StepVerifier::create)
                .assertNext(md5 -> {
                    assertNotNull(md5);
                    assertFalse(md5.isEmpty());
                })
                .verifyComplete();

        // SHA-256
        reactiveCryptoService.sha256(input)
                .as(StepVerifier::create)
                .assertNext(sha256 -> {
                    assertNotNull(sha256);
                    assertFalse(sha256.isEmpty());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Reactive AES 加解密测试")
    void testReactiveAesEncryptDecrypt() {
        String data = "sensitive-data-123";

        reactiveCryptoService.generateAesKey()
                .flatMap(key -> reactiveCryptoService.aesEncrypt(data, key)
                        .flatMap(encrypted -> reactiveCryptoService.aesDecrypt(encrypted, key)))
                .as(StepVerifier::create)
                .assertNext(decrypted -> assertEquals(data, decrypted, "解密后数据应一致"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Reactive RSA 密钥对生成测试")
    void testReactiveRsaKeyPair() {
        reactiveCryptoService.generateRsaKeyPair()
                .as(StepVerifier::create)
                .assertNext(keyPair -> {
                    assertNotNull(keyPair);
                    assertEquals(2, keyPair.length, "应该包含公钥和私钥");
                    assertNotNull(keyPair[0], "公钥不应为空");
                    assertNotNull(keyPair[1], "私钥不应为空");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Reactive 随机密钥和盐值生成测试")
    void testReactiveRandomGeneration() {
        reactiveCryptoService.generateRandomKey(32)
                .as(StepVerifier::create)
                .assertNext(key -> {
                    assertNotNull(key);
                    assertFalse(key.isEmpty());
                })
                .verifyComplete();

        reactiveCryptoService.generateSalt()
                .as(StepVerifier::create)
                .assertNext(salt -> {
                    assertNotNull(salt);
                    assertFalse(salt.isEmpty());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Reactive 登录测试")
    void testReactiveLogin() {
        AuthRequest request = new AuthRequest();
        request.setUsername("admin");
        request.setPassword("{bcrypt}$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        request.setClientIp("127.0.0.1");
        request.setDeviceId("reactive-device-001");

        StepVerifier.create(reactiveAuthService.login(request))
                .assertNext(response -> assertNotNull(response, "登录响应不应为 null"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Reactive Token 验证测试")
    void testReactiveVerifyToken() {
        // 生成 Token 后验证
        java.util.Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("userId", "1");
        claims.put("username", "admin");

        reactiveTokenService.generateToken("1", claims)
                .flatMap(reactiveAuthService::verifyToken)
                .as(StepVerifier::create)
                .assertNext(valid -> assertTrue(valid, "生成的 Token 应该有效"))
                .verifyComplete();
    }
}
