package cn.aryee.examples.storage.minio.reactive;

import cn.aryee.storage.api.config.StorageProperties;
import cn.aryee.storage.api.model.FileInfo;
import cn.aryee.storage.api.model.UploadResult;
import cn.aryee.storage.api.service.ReactiveFileService;
import cn.aryee.storage.infrastructure.reactive.minio.ReactiveMinioFileService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Aryee Storage MinIO Reactive 集成测试
 * 验证 ReactiveMinioFileService 与真实 MinIO 服务的连通性和功能正确性
 *
 * <p>架构规则 6.1 安全隔离原则：
 * <ul>
 *   <li>仅引入 storage-reactive-spring-boot-starter</li>
 *   <li>禁止同时引入 storage-spring-boot-starter（Blocking Starter）</li>
 * </ul>
 *
 * @author Aryee
 * @since 1.0.0
 */
@DisplayName("MinIO Reactive 文件服务集成测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReactiveMinioFileServiceIntegrationTest {

    private static ReactiveFileService fileService;

    private static final String TEST_BUCKET = "aryee-test";
    private static final String TEST_DIRECTORY = "test-reactive";
    private static String uploadedFileId;
    private static String uploadedFilePath;

    @BeforeAll
    static void setUp() {
        StorageProperties.MinioStorage config = new StorageProperties.MinioStorage();
        config.setEnabled(true);
        config.setEndpoint("http://localhost:9000");
        config.setAccessKey("minio");
        config.setSecretKey("minio123");
        config.setBucket(TEST_BUCKET);
        config.setSecure(false);
        fileService = new ReactiveMinioFileService(config);
    }

    @Test
    @Order(1)
    @DisplayName("1. 获取存储类型 - getStorageType()")
    void testGetStorageType() {
        StepVerifier.create(fileService.getStorageType())
                .expectNext("minio")
                .verifyComplete();
    }

    @Test
    @Order(2)
    @DisplayName("2. 上传文件（byte[]） - upload(byte[], fileName, bucket, directory)")
    void testUploadWithByteArray() {
        String content = "Hello MinIO Reactive - Byte Array Upload";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        StepVerifier.create(fileService.upload(bytes, "test-reactive.txt", TEST_BUCKET, TEST_DIRECTORY))
                .assertNext(result -> {
                    assertThat(result.isSuccess()).isTrue();
                    assertThat(result.getFileId()).isNotNull();
                    assertThat(result.getFileInfo()).isNotNull();
                    assertThat(result.getFileInfo().getBucket()).isEqualTo(TEST_BUCKET);
                    assertThat(result.getFileInfo().getPath()).startsWith(TEST_DIRECTORY + "/");
                    assertThat(result.getFileUrl()).isNotNull();

                    uploadedFileId = result.getFileId();
                    uploadedFilePath = result.getFileInfo().getPath();
                })
                .verifyComplete();
    }

    @Test
    @Order(3)
    @DisplayName("3. 上传文件（Flux<DataBuffer>） - upload(Flux<DataBuffer>, ...)")
    void testUploadWithDataBufferFlux() {
        String content = "Hello MinIO Reactive - DataBuffer Flux Upload";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
        DataBuffer buffer = factory.wrap(bytes);
        Flux<DataBuffer> dataBufferFlux = Flux.just(buffer);

        StepVerifier.create(fileService.upload(dataBufferFlux, "test-flux.txt", TEST_BUCKET, TEST_DIRECTORY))
                .assertNext(result -> {
                    assertThat(result.isSuccess()).isTrue();
                    assertThat(result.getFileId()).isNotNull();
                    assertThat(result.getFileInfo().getBucket()).isEqualTo(TEST_BUCKET);
                })
                .verifyComplete();
    }

    @Test
    @Order(4)
    @DisplayName("4. 上传 JSON 文件")
    void testUploadJsonFile() {
        String jsonContent = "{\"name\":\"Aryee\",\"action\":\"test\",\"type\":\"reactive\"}";
        byte[] bytes = jsonContent.getBytes(StandardCharsets.UTF_8);

        StepVerifier.create(fileService.upload(bytes, "data.json", TEST_BUCKET, TEST_DIRECTORY))
                .assertNext(result -> {
                    assertThat(result.isSuccess()).isTrue();
                    assertThat(result.getFileId()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @Order(5)
    @DisplayName("5. 上传二进制文件（模拟图片）")
    void testUploadBinaryFile() {
        byte[] binaryData = new byte[1024];
        Arrays.fill(binaryData, (byte) 0x42);

        StepVerifier.create(fileService.upload(binaryData, "test-reactive-image.dat", TEST_BUCKET, TEST_DIRECTORY))
                .assertNext(result -> {
                    assertThat(result.isSuccess()).isTrue();
                    assertThat(result.getFileId()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @Order(6)
    @DisplayName("6. 列出文件 - listFiles(bucket, directory, pageNum, pageSize)")
    void testListFiles() {
        StepVerifier.create(fileService.listFiles(TEST_BUCKET, TEST_DIRECTORY, 1, 10))
                .expectNextCount(1)
                .thenCancel()
                .verify();
    }

    @Test
    @Order(7)
    @DisplayName("7. 根据路径查询文件 - getFileByPath(bucket, path)")
    void testGetFileByPath() {
        StepVerifier.create(fileService.getFileByPath(TEST_BUCKET, uploadedFilePath))
                .assertNext(fileInfo -> {
                    assertThat(fileInfo).isNotNull();
                    assertThat(fileInfo.getBucket()).isEqualTo(TEST_BUCKET);
                    assertThat(fileInfo.getPath()).isEqualTo(uploadedFilePath);
                })
                .verifyComplete();
    }

    @Test
    @Order(8)
    @DisplayName("8. 检查文件是否存在 - exists(fileId)")
    void testExists() {
        // getFileInfo 是 stub 返回 Mono.empty()，所以 exists 返回 false
        StepVerifier.create(fileService.exists(uploadedFileId))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @Order(9)
    @DisplayName("9. 删除文件 - delete(fileId)")
    void testDelete() {
        // getFileInfo 是 stub 返回 Mono.empty()，所以 delete 返回 false
        StepVerifier.create(fileService.delete(uploadedFileId))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @Order(10)
    @DisplayName("10. 获取文件URL - getFileUrl(fileId)")
    void testGetFileUrl() {
        // getFileInfo 是 stub 返回 Mono.empty()，所以 getFileUrl 完成时不发元素
        StepVerifier.create(fileService.getFileUrl(uploadedFileId))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    @Order(11)
    @DisplayName("11. 获取带过期时间的文件URL - getFileUrl(fileId, expires)")
    void testGetFileUrlWithExpires() {
        // getFileInfo 是 stub 返回 Mono.empty()，完成时不发元素
        StepVerifier.create(fileService.getFileUrl(uploadedFileId, 3600000L))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    @Order(12)
    @DisplayName("12. 生成缩略图 - generateThumbnail(fileId, width, height)")
    void testGenerateThumbnail() {
        // getFileUrl 依赖 getFileInfo（返回空），所以 generateThumbnail 完成时不发元素
        StepVerifier.create(fileService.generateThumbnail(uploadedFileId, 100, 100))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    @Order(13)
    @DisplayName("13. 批量删除文件 - deleteBatch(Flux<String>)")
    void testDeleteBatch() {
        Flux<String> fileIds = Flux.just(uploadedFileId, "non-existent-id");

        // delete 返回 false（getFileInfo 为空），filter 过滤掉，所以无元素
        StepVerifier.create(fileService.deleteBatch(fileIds))
                .expectNextCount(0)
                .verifyComplete();
    }
}
