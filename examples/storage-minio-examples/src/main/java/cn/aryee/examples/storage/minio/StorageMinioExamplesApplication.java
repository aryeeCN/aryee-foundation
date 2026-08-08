package cn.aryee.examples.storage.minio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aryee Storage MinIO Blocking 模式示例应用启动类
 *
 * <p>依赖说明：
 * <ul>
 *   <li>仅引入 storage-spring-boot-starter（Blocking Starter）</li>
 *   <li>禁止同时引入 storage-reactive-spring-boot-starter（架构规则 6.1 Starter 隔离）</li>
 * </ul>
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootApplication
public class StorageMinioExamplesApplication {

    public static void main(String[] args) {
        SpringApplication.run(StorageMinioExamplesApplication.class, args);
    }
}
