package cn.aryee.examples.cache.blocking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Aryee Cache Blocking 模式示例应用启动类。
 * <p>
 * 通过 {@code @EnableCaching} 开启 Spring 声明式缓存支持，
 * 配合 {@code AryeeCacheManager} 自动桥接 Aryee {@link cn.aryee.cache.api.service.CacheService}。
 * </p>
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootApplication
@EnableCaching
public class CacheExamplesBlockingApplication {

    public static void main(String[] args) {
        SpringApplication.run(CacheExamplesBlockingApplication.class, args);
    }
}
