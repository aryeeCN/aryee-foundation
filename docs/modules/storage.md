# Aryee Storage 文件存储基础设施模块

> **所属项目**: [Aryee Foundation](../../README.md)
> **架构层次**: 基础设施层 (Foundation Layer)
> **技术栈**: Java 21, Spring Boot 4.0.6, Spring Cloud 2025.1.2
> **存储后端**: Local / OSS / COS / MinIO / Qiniu
> **访问模式**: 阻塞式 (Blocking) / 响应式 (Reactive)

## 简介

文件存储基础设施模块基于三层架构（API / Infrastructure / Autoconfigure）实现，提供统一的文件服务抽象层。支持本地存储、阿里云 OSS、腾讯云 COS、MinIO、七牛云五种存储后端，每种后端均提供 Blocking 与 Reactive 双模式实现，接口与实现完全隔离，切换存储后端无需修改业务代码。

### 核心特性

- ✅ **三层架构**: API 契约层 + Infrastructure 实现层 + Autoconfigure 自动配置层
- ✅ **五种存储后端**: Local / OSS / COS / MinIO / Qiniu
- ✅ **双模式隔离**: Blocking（`FileService`）与 Reactive（`ReactiveFileService`）接口与实现完全隔离，使用独立 Starter
- ✅ **实现同步原则**: 每种存储后端都同时提供 Blocking 和 Reactive 版本
- ✅ **统一服务接口**: 上传、下载、删除、复制、移动、查询、签名 URL、缩略图
- ✅ **Reactive 等价能力**: Blocking `upload(MultipartFile/InputStream)` ↔ Reactive `upload(byte[]/Flux<DataBuffer>)`；Blocking `downloadToStream` ↔ Reactive `downloadContent` 返回 `Mono<byte[]>`
- ✅ **文件安全**: 类型校验、大小限制、校验值（MD5/SHA256）、签名 URL
- ✅ **安全管控（可选）**: 委托 security 模块进行文件操作权限检查和审计日志（`aryee.storage.security.enabled=true`）
- ✅ **缩略图**: 内置缩略图生成服务
- ✅ **Spring Boot 自动配置**: 通过 `AutoConfiguration.imports` 自动装配

## 模块结构

```
aryee-foundation-storage/
├── storage-api/                                # API 契约层
│   └── cn.aryee.storage.api
│       ├── config/                             # 配置属性
│       │   ├── StorageProperties.java          # 配置前缀 aryee.storage
│       │   ├── FileProperties.java             # 配置前缀 aryee.file
│       │   └── model/                          # 各存储后端配置
│       │       ├── LocalConfig.java
│       │       ├── OssConfig.java
│       │       ├── CosConfig.java
│       │       ├── MinioConfig.java
│       │       ├── QiniuConfig.java
│       │       └── ReactiveConfig.java
│       ├── constant/FileConstants.java
│       ├── enums/                              # FileStatus / ThumbnailSize
│       ├── exception/                          # StorageException / FileOperationException
│       ├── model/                              # 数据模型
│       │   ├── FileInfo.java
│       │   ├── UploadResult.java
│       │   ├── FileMetadata.java
│       │   ├── Attachment.java
│       │   ├── StorageFile.java
│       │   ├── FileQuery.java
│       │   ├── FileUploadRequest.java
│       │   └── FileOperations.java
│       ├── service/                            # 服务契约
│       │   ├── FileService.java                # Blocking 主接口
│       │   ├── ReactiveFileService.java        # Reactive 主接口
│       │   ├── FileServiceFactory.java
│       │   ├── ReactiveFileServiceFactory.java
│       │   └── ThumbnailService.java
│       ├── support/lookup/                     # 服务发现
│       │   ├── FileServiceLoader.java
│       │   ├── FileServiceLookup.java
│       │   └── ReactiveFileServiceLookup.java
│       ├── thumbnail/ThumbnailGenerator.java
│       └── util/                               # FileUtil / IoUtil / OutputStreamUtil / ZipUtil
│
├── storage-infrastructure/                     # 实现层
│   └── cn.aryee.storage.infrastructure
│       ├── blocking/                           # Blocking 实现（每种后端一份）
│       │   ├── BlockingStorageService.java
│       │   ├── local/LocalFileService.java, LocalFileServiceFactory.java
│       │   ├── oss/OssFileService.java
│       │   ├── cos/CosFileService.java
│       │   ├── minio/MinioFileService.java, MinioFileServiceFactory.java
│       │   └── qiniu/QiniuFileService.java, QiniuFileServiceFactory.java
│       └── reactive/                           # Reactive 实现（每种后端一份）
│           ├── local/ReactiveLocalFileService.java, ReactiveLocalFileServiceFactory.java
│           ├── oss/ReactiveOssFileService.java
│           ├── cos/ReactiveCosFileService.java
│           ├── minio/ReactiveMinioFileService.java, ReactiveMinioFileServiceFactory.java
│           └── qiniu/ReactiveQiniuFileService.java, ReactiveQiniuFileServiceFactory.java
│
├── storage-spring-boot-autoconfigure/          # Blocking 自动配置
│   └── cn.aryee.storage.autoconfigure
│       └── AryeeStorageAutoConfiguration.java
│
├── storage-reactive-spring-boot-autoconfigure/ # Reactive 自动配置
│   └── cn.aryee.storage.reactive.autoconfigure
│       └── AryeeStorageReactiveAutoConfiguration.java
│
├── storage-spring-boot-starter/                # Blocking Starter
└── storage-reactive-spring-boot-starter/       # Reactive Starter
```

**自动配置注册**：
- Blocking: `cn.aryee.storage.autoconfigure.AryeeStorageAutoConfiguration`
- Reactive: `cn.aryee.storage.reactive.autoconfigure.AryeeStorageReactiveAutoConfiguration`

## 使用方法

### 1. 引入 BOM（推荐）

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>cn.aryee.foundation</groupId>
            <artifactId>bom-full</artifactId>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 2. 按需引入 Starter

```xml
<!-- 阻塞式（Servlet / WebMVC 场景） -->
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>storage-spring-boot-starter</artifactId>
</dependency>

<!-- 或：响应式（WebFlux 场景，二选一，禁止同时引入） -->
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>storage-reactive-spring-boot-starter</artifactId>
</dependency>
```

## 配置

配置前缀：`aryee.storage`（也兼容 `aryee.file`）

```yaml
aryee:
  storage:
    # 是否启用存储服务
    enabled: true
    # 默认存储类型：local / oss / minio / cos / qiniu
    default-type: local
    # 默认存储桶名称
    default-bucket: default
    # 默认文件访问 URL 前缀
    default-url-prefix: /files
    # 最大文件大小（字节），默认 100MB
    max-file-size: 104857600
    # 最大图片文件大小（字节），默认 10MB
    max-image-size: 10485760
    # 是否启用文件校验
    validation-enabled: true
    # 是否生成缩略图
    thumbnail-enabled: false
    thumbnail-width: 200
    thumbnail-height: 200
    # 是否生成文件校验值（MD5/SHA256）
    checksum-enabled: false
    # 是否启用文件缓存
    cache-enabled: false
    cache-expire-seconds: 3600

    # 本地存储配置
    local:
      enabled: true
      storage-path: ./data/storage
      url-prefix: /files

    # 阿里云 OSS 配置
    oss:
      enabled: false
      endpoint: oss-cn-hangzhou.aliyuncs.com
      access-key: your-access-key
      secret-key: your-secret-key
      bucket: your-bucket
      custom-domain: https://cdn.example.com
      secure: true

    # 腾讯云 COS 配置
    cos:
      enabled: false
      region: ap-guangzhou
      secret-id: your-secret-id
      secret-key: your-secret-key
      bucket: your-bucket-1250000000
      custom-domain: https://cdn.example.com

    # MinIO 配置
    minio:
      enabled: false
      endpoint: http://localhost:9000
      access-key: minioadmin
      secret-key: minioadmin
      bucket: default-bucket
      secure: false

    # 七牛云配置
    qiniu:
      enabled: false
      access-key: your-access-key
      secret-key: your-secret-key
      bucket: your-bucket
      region: z0
      custom-domain: https://cdn.example.com
```

## 代码示例

### Blocking 模式

```java
@Service
public class FileStorageService {

    private final FileService fileService;

    public FileStorageService(FileService fileService) {
        this.fileService = fileService;
    }

    // 1. 上传文件（MultipartFile）
    public UploadResult upload(MultipartFile file, String directory) {
        return fileService.upload(file, "default-bucket", directory);
    }

    // 2. 上传文件（InputStream）
    public UploadResult upload(InputStream inputStream, String fileName, String directory) {
        return fileService.upload(inputStream, fileName, "default-bucket", directory);
    }

    // 3. 下载文件到输出流
    public void downloadToStream(String fileId, OutputStream outputStream) {
        fileService.downloadToStream(fileId, outputStream);
    }

    // 4. 获取文件信息
    public FileInfo getFileInfo(String fileId) {
        return fileService.getFileInfo(fileId);
    }

    // 5. 生成签名 URL（带过期时间）
    public String getPresignedUrl(String fileId, long expiresSeconds) {
        return fileService.getFileUrl(fileId, expiresSeconds);
    }

    // 6. 复制 / 移动文件
    public FileInfo copyFile(String fileId, String targetBucket, String targetPath) {
        return fileService.copy(fileId, targetBucket, targetPath);
    }

    // 7. 生成缩略图
    public String generateThumbnail(String fileId, int width, int height) {
        return fileService.generateThumbnail(fileId, width, height);
    }

    // 8. 批量删除
    public List<String> deleteBatch(List<String> fileIds) {
        return fileService.deleteBatch(fileIds);
    }

    // 9. 分页列出文件
    public List<FileInfo> listFiles(String bucket, String directory, int pageNum, int pageSize) {
        return fileService.listFiles(bucket, directory, pageNum, pageSize);
    }
}
```

### Reactive 模式

```java
@Service
public class ReactiveFileStorageService {

    private final ReactiveFileService reactiveFileService;

    public ReactiveFileStorageService(ReactiveFileService reactiveFileService) {
        this.reactiveFileService = reactiveFileService;
    }

    // 1. 上传文件（字节数组）
    public Mono<UploadResult> upload(byte[] content, String fileName, String directory) {
        return reactiveFileService.upload(content, fileName, "default-bucket", directory);
    }

    // 2. 上传文件（Flux<DataBuffer>，适用于 WebFlux FilePart）
    public Mono<UploadResult> upload(Flux<DataBuffer> content, String fileName, String directory) {
        return reactiveFileService.upload(content, fileName, "default-bucket", directory);
    }

    // 3. 下载文件内容（返回字节数组，等价于 Blocking 的 downloadToStream）
    public Mono<byte[]> downloadContent(String fileId) {
        return reactiveFileService.downloadContent(fileId);
    }

    // 4. 获取文件信息
    public Mono<FileInfo> getFileInfo(String fileId) {
        return reactiveFileService.getFileInfo(fileId);
    }

    // 5. 生成签名 URL
    public Mono<String> getPresignedUrl(String fileId, long expiresSeconds) {
        return reactiveFileService.getFileUrl(fileId, expiresSeconds);
    }

    // 6. 复制 / 移动
    public Mono<FileInfo> copyFile(String fileId, String targetBucket, String targetPath) {
        return reactiveFileService.copy(fileId, targetBucket, targetPath);
    }

    // 7. 生成缩略图
    public Mono<String> generateThumbnail(String fileId, int width, int height) {
        return reactiveFileService.generateThumbnail(fileId, width, height);
    }

    // 8. 批量删除（返回成功删除的 ID 流）
    public Flux<String> deleteBatch(Flux<String> fileIds) {
        return reactiveFileService.deleteBatch(fileIds);
    }

    // 9. 分页列出文件
    public Flux<FileInfo> listFiles(String bucket, String directory, int pageNum, int pageSize) {
        return reactiveFileService.listFiles(bucket, directory, pageNum, pageSize);
    }
}
```

### WebFlux Controller 集成示例

```java
@RestController
@RequestMapping("/files")
public class FileController {

    private final ReactiveFileService reactiveFileService;

    public FileController(ReactiveFileService reactiveFileService) {
        this.reactiveFileService = reactiveFileService;
    }

    @PostMapping("/upload")
    public Mono<UploadResult> upload(@RequestPart("file") FilePart filePart) {
        return reactiveFileService.upload(
                filePart.content(),
                filePart.filename(),
                "default-bucket",
                "uploads"
        );
    }

    @GetMapping("/download/{fileId}")
    public Mono<ResponseEntity<byte[]>> download(@PathVariable String fileId) {
        return reactiveFileService.downloadContent(fileId)
                .map(bytes -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment")
                        .body(bytes));
    }
}
```

## 安全管控

存储模块支持可选的安全管控能力，委托 [security 模块](security.md) 进行文件操作权限检查和审计日志，遵循 [security-governance.md](https://github.com/aryeecn/aryee-foundation)（内部规范：security-governance） 规则。

### 工作原理

```
调用方 → SecuredFileService（装饰器） → 原始 FileService
              ↓                            ↓
     StorageSecurityService          文件上传/下载/删除
      ├─ checkPermission()           （委托 security 模块）
      └─ audit()
```

### 装配条件

| Bean | 条件 | 说明 |
|------|------|------|
| `DefaultStorageSecurityService` | `DynamicPermissionService` + `SecurityAuditService` Bean 存在 + `security.enabled=true` | security 模块可用时，委托权限检查和审计 |
| `NoopStorageSecurityService` | `security.enabled=true` 但 security 模块未引入 | 降级方案，不检查权限不记录日志 |
| `SecuredFileService` | `StorageSecurityService` Bean 存在 + `security.enabled=true` | `@Primary` 装饰器，自动包装原始 FileService |

### 配置示例

```yaml
aryee:
  storage:
    security:
      enabled: true              # 启用安全管控
      audit-enabled: true        # 启用操作审计日志
      credential-encrypted: false # 凭证是否已加密（启用后 accessKey/secretKey 将在启动时解密）
```

### 权限映射

| 存储权限常量 | security action | 对应操作 |
|-------------|----------------|---------|
| `file:read` | `read` | 下载、查询、列出文件 |
| `file:write` | `create` | 上传、复制、移动 |
| `file:delete` | `delete` | 删除、批量删除 |
| `file:share` | `read` | 获取带过期时间的签名 URL |

### 审计日志

文件操作审计日志通过 `SecurityAuditService.logDataAccess()` 记录，包含：
- 操作人 ID
- 操作类型（UPLOAD / DOWNLOAD / DELETE / COPY / MOVE / GET_URL）
- 目标文件 ID
- 存储类型（local / oss / minio / cos / qiniu）
- 操作结果（成功 / 失败）

### 使用方式

在调用文件服务前设置当前用户 ID：

```java
SecurityContextHolder.setUserId(currentUserId);
try {
    fileService.upload(file, bucket, directory);
} finally {
    SecurityContextHolder.clear();
}
```

## 兼容性

| 存储后端 | Blocking | Reactive | SDK |
|----------|----------|----------|-----|
| Local（本地文件系统） | ✅ | ✅ | JDK NIO |
| 阿里云 OSS | ✅ | ✅ | OSS SDK 3.x |
| 腾讯云 COS | ✅ | ✅ | COS SDK 5.x |
| MinIO | ✅ | ✅ | MinIO SDK 8.x |
| 七牛云 Qiniu | ✅ | ✅ | Qiniu SDK 7.x |

| 环境 | 版本要求 |
|------|----------|
| JDK | 21+ |
| Spring Boot | 4.0.6 |
| Spring Cloud | 2025.1.2 |
| Jakarta EE | 9+ |

### Blocking vs Reactive 选型

- **Blocking 场景**: Servlet/WebMVC 应用、`MultipartFile` 上传、`OutputStream` 流式下载（管理后台、内容管理系统）
- **Reactive 场景**: WebFlux 应用、`FilePart` + `Flux<DataBuffer>` 全链路非阻塞（高并发文件网关、CDN 回源服务）

> **重要**: Blocking 与 Reactive Starter 必须二选一，禁止同时引入。每种存储后端都同时提供 Blocking 与 Reactive 实现，不会出现某后端只有单模式支持的情况。
