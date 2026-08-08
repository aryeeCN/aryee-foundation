package cn.aryee.examples.gateway.reactive.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sa-Token 网关鉴权配置
 * <p>
 * 完整复现 aryee-cloud-gateway 项目的 SaTokenConfigure 配置，提供以下能力：
 * <ul>
 *   <li>全局路由拦截：拦截 {@code /**}，排除静态资源和文档路径</li>
 *   <li>登录校验：非白名单路径需要 {@code StpUtil.checkLogin()}</li>
 *   <li>异常处理：返回 JSON 格式错误响应 {@code {"code":401,"msg":"未登录"}}</li>
 *   <li>跨域配置：在 {@code setBeforeAuth} 中设置 CORS 响应头</li>
 * </ul>
 *
 * @author Aryee
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(SaTokenGatewayConfig.SaTokenGatewayProperties.class)
public class SaTokenGatewayConfig {

    /**
     * 注册 Sa-Token 响应式全局过滤器
     * <p>
     * 拦截全部路径并排除静态资源、文档路径，对非白名单接口执行登录校验。
     * 在前置函数中统一设置 CORS 跨域响应头，确保所有请求（包括预检请求）都能正确返回跨域头。
     *
     * @param properties Sa-Token 网关配置属性
     * @return Sa-Token 响应式过滤器
     */
    @Bean
    public SaReactorFilter saReactorFilter(SaTokenGatewayProperties properties) {
        return new SaReactorFilter()
            // 拦截全部路径
            .addInclude("/**")
            // 排除静态资源和文档路径
            .addExclude(properties.getExcludes().toArray(new String[0]))
            // 前置函数：在每次认证函数之前执行，设置 CORS 跨域响应头
            .setBeforeAuth(obj -> {
                SaHolder.getResponse()
                    .setHeader("Access-Control-Allow-Origin", "*")
                    .setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE")
                    .setHeader("Access-Control-Max-Age", "3600")
                    .setHeader("Access-Control-Allow-Headers", "*");
            })
            // 认证函数：每次请求执行，对非白名单路径校验登录状态
            .setAuth(obj -> {
                String[] openPaths = properties.getOpenPaths().toArray(new String[0]);
                SaRouter.match("/**")
                    .notMatch(openPaths)
                    .check(r -> StpUtil.checkLogin());
            })
            // 异常处理函数：认证函数发生异常时执行，返回 JSON 格式错误响应
            .setError(e -> {
                Map<String, Object> result = new HashMap<>(2);
                result.put("code", 401);
                result.put("msg", "未登录");
                return result;
            });
    }

    /**
     * Sa-Token 网关配置属性
     * <p>
     * 管理排除路径（静态资源、文档）和开放接口列表（无需登录的接口）。
     * 配置前缀：{@code sa-token.gateway}
     *
     * @author Aryee
     * @since 1.0.0
     */
    @Data
    @ConfigurationProperties(prefix = "sa-token.gateway")
    public static class SaTokenGatewayProperties {

        /**
         * 排除路径列表（静态资源、文档路径）
         * <p>
         * 这些路径不经过 Sa-Token 过滤器，直接放行。
         */
        private List<String> excludes = new ArrayList<>(List.of(
            "/favicon.ico",
            "/doc.html",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**"
        ));

        /**
         * 开放接口列表（无需登录的接口）
         * <p>
         * 这些路径经过 Sa-Token 过滤器，但跳过登录校验。
         */
        private List<String> openPaths = new ArrayList<>(List.of(
            "/auth/login",
            "/auth/register"
        ));
    }
}
