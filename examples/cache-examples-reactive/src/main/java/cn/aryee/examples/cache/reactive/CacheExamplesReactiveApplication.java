package cn.aryee.examples.cache.reactive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aryee Cache Reactive 模式示例应用启动类。
 * <p>
 * 基于 Spring WebFlux + ReactiveCacheService，
 * 所有缓存操作返回 {@code Mono}/{@code Flux}，全程非阻塞。
 * </p>
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootApplication
public class CacheExamplesReactiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(CacheExamplesReactiveApplication.class, args);
    }
}
