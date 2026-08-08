# Aryee Foundation CLI

> **所属项目**: [Aryee Foundation](../../README.md)
> **类型**: 命令行工具（Fat JAR）
> **技术栈**: Java 21, Spring Boot 4.0.7, Picocli, FreeMarker, Jasypt, SnakeYAML
> **主类**: `cn.aryee.cli.FoundationCli`

Aryee Foundation 脚手架与代码生成工具，提供项目初始化、代码生成（后端 + SQL + 前端 Vue 全套）、模块骨架创建、项目校验、依赖分析和配置加密等命令行能力。

## 快速开始

### 构建

```bash
mvn clean package -DskipTests
```

构建产物为 `target/foundation-cli.jar`（通过 `maven-shade-plugin` 打包，包含所有依赖，可直接 `java -jar` 运行）。

### 运行

```bash
java -jar target/foundation-cli.jar <command> [options]
```

查看帮助：

```bash
java -jar target/foundation-cli.jar --help
```

## 命令一览

CLI 入口由 `@Command(name = "foundation")` 注册，命令清单：

| 命令 | 来源类 | 说明 |
|------|--------|------|
| `version` | `VersionCommand` | 显示 CLI 版本、Java 运行时、OS 等环境信息 |
| `init` | `FoundationCli.init` | 初始化一个基于 Foundation 的新项目 |
| `generate` | `FoundationCli.generate` | 基于 FreeMarker 模板生成单类型代码（后端/SQL/前端） |
| `gen-all` | `FoundationCli.generateAll` | 一键生成全套代码（后端 6 类 + SQL 2 类 + 前端 2 类） |
| `module` | `FoundationCli.module` | 在当前项目中创建新的 Foundation 功能模块骨架 |
| `check` | `ProjectValidator` | 校验项目结构是否符合 Foundation 标准 |
| `deps` | `DependencyAnalyzer` | 扫描项目中所有 POM，分析依赖分布情况 |
| `crypto` | `CryptoCommand` | 配置加密工具（含 `encrypt` / `decrypt` / `generate-key` 子命令） |

> 顶层命令通过 `@Command(subcommands = {...})` 注册，`init` / `generate` / `gen-all` / `module` 是 `FoundationCli` 内的 `@Command` 方法。

---

## 命令详解

### version

显示 CLI 及运行环境版本信息。

```bash
java -jar foundation-cli.jar version
```

输出示例：

```
╔════════════════════════════════════════╗
║       Aryee Foundation CLI              ║
╚════════════════════════════════════════╝
--------------------------------------------------
CLI Version:    1.0.0
Foundation:     1.0.0-SNAPSHOT
Java Version:   21
Java Vendor:    Oracle Corporation
OS:             macOS 14.5
Architecture:   aarch64
User Dir:       /Users/aryee/my-project
```

> Foundation 版本通过读取 `META-INF/maven/cn.aryee.foundation/aryee-foundation-cli/pom.properties` 获取。

### init

初始化一个基于 Foundation 的新项目，自动创建标准目录结构、`pom.xml`、Spring Boot 启动类和 `application.yml`。

```bash
java -jar foundation-cli.jar init \
  -n my-project \
  -p com.example.myproject
```

**参数**：

| 参数 | 说明 | 必填 | 默认值 |
|------|------|------|--------|
| `-n, --name` | 项目名称 | 是 | - |
| `-p, --package` | 基础包名 | 否 | `com.example.{name.toLowerCase()}` |

**生成的文件**：

```
my-project/
├── pom.xml                                       ← parent 指向 foundation-spring-boot-starter
└── src/main/
    ├── java/com/example/myproject/
    │   └── MyProjectApplication.java             ← @SpringBootApplication 启动类
    └── resources/
        └── application.yml                       ← server.port=8080 + spring.application.name
```

### generate

基于 FreeMarker 模板生成标准化的代码文件，支持 10 种类型：
- 后端 6 类：`controller` / `service`（接口+实现） / `entity` / `dto` / `repository` / `mapper`
- SQL 2 类：`menu`（菜单+按钮 SQL） / `permission`（权限+数据权限 SQL）
- 前端 2 类：`vue`（列表页） / `vue-form`（表单弹窗）

```bash
# ===== 后端 =====
# 生成 Controller
java -jar foundation-cli.jar generate -t controller -n User -p com.example.demo

# 生成 Service（接口 + 实现）
java -jar foundation-cli.jar generate -t service -n User -p com.example.demo

# 生成 Entity
java -jar foundation-cli.jar generate -t entity -n User -p com.example.demo

# 生成 DTO
java -jar foundation-cli.jar generate -t dto -n User -p com.example.demo

# 生成 Repository
java -jar foundation-cli.jar generate -t repository -n User -p com.example.demo

# 生成 Mapper
java -jar foundation-cli.jar generate -t mapper -n User -p com.example.demo

# ===== SQL =====
# 生成菜单+按钮 SQL（依赖字段定义和中文名）
java -jar foundation-cli.jar generate -t menu -n User \
  -f "username:用户名,age:年龄" -c "用户管理"

# 生成权限+数据权限 SQL
java -jar foundation-cli.jar generate -t permission -n User -c "用户管理"

# ===== 前端 =====
# 生成 Vue3 列表页（依赖字段定义）
java -jar foundation-cli.jar generate -t vue -n User \
  -f "username:用户名,age:年龄" -c "用户管理"

# 生成 Vue3 表单弹窗
java -jar foundation-cli.jar generate -t vue-form -n User \
  -f "username:用户名,age:年龄" -c "用户管理"
```

**参数**：

| 参数 | 说明 | 必填 | 默认值 |
|------|------|------|--------|
| `-t, --type` | 生成类型：`controller` / `service` / `entity` / `dto` / `repository` / `mapper` / `menu` / `permission` / `vue` / `vue-form` | 是 | - |
| `-n, --name` | 类名（不含后缀） | 是 | - |
| `-p, --package` | 目标包名（仅后端类型使用） | 否 | `com.example` |
| `-f, --fields` | 字段定义（格式：`name:label,name:label`，如 `username:用户名,age:年龄`）；menu/vue/vue-form 必填 | 否 | - |
| `-c, --cn-name` | 中文名（用于 menu/permission 描述） | 否 | `${name}管理` |

**模板列表**（`src/main/resources/templates/`）：

| 模板文件 | 生成类型 | 输出文件 | 输出目录 |
|----------|----------|----------|----------|
| `controller.ftl` | Controller | `{Name}Controller.java` | `controller/` |
| `service.ftl` | Service 接口 | `{Name}Service.java` | `service/` |
| `service-impl.ftl` | Service 实现 | `{Name}ServiceImpl.java` | `service/impl/` |
| `entity.ftl` | Entity | `{Name}.java` | `entity/` |
| `dto.ftl` | DTO | `{Name}.java` | `dto/` |
| `repository.ftl` | Repository | `{Name}Repository.java` | `repository/` |
| `mapper.ftl` | Mapper | `{Name}Mapper.java` | `mapper/` |
| `menu.ftl` | 菜单+按钮 SQL | `{name}_menu.sql` | `resources/sql/` |
| `permission.ftl` | 权限+数据权限 SQL | `{name}_permission.sql` | `resources/sql/` |
| `vue.ftl` | Vue3 列表页 | `index.vue` | `resources/frontend/views/{name-kebab}/` |
| `vue-form.ftl` | Vue3 表单弹窗 | `{name-kebab}-form.vue` | `resources/frontend/views/{name-kebab}/` |

> `CodeGenerator` 使用 FreeMarker `Configuration.VERSION_2_3_32`，通过 `setClassForTemplateLoading(getClass(), "/templates")` 从 classpath 加载模板，编码统一为 UTF-8。
>
> 模板变量：`name`（类名）、`nameLower`（首字母小写）、`nameKebab`（kebab-case）、`nameCn`（中文名）、`basePackage`（包名）、`fields`（字段列表，每项含 `name` / `label`）。

### gen-all

一键生成全套代码（后端 + SQL + 前端），等价于依次执行 10 次 `generate` 命令。适用于快速搭建 CRUD 模块原型。

```bash
java -jar foundation-cli.jar gen-all -n User \
  -p com.example.demo \
  -f "username:用户名,age:年龄,email:邮箱,createdAt:创建时间" \
  -c "用户管理"
```

**参数**：

| 参数 | 说明 | 必填 | 默认值 |
|------|------|------|--------|
| `-n, --name` | 实体名（如 `User`） | 是 | - |
| `-p, --package` | 基础包名 | 否 | `com.example` |
| `-f, --fields` | 字段定义（格式：`name:label,name:label`） | 是 | - |
| `-c, --cn-name` | 中文名（用于菜单/权限描述） | 否 | `${name}管理` |

**生成的文件**（共 10 个）：

```
src/main/
├── java/{package}/                                ← 后端 6 类
│   ├── controller/UserController.java
│   ├── entity/User.java
│   ├── dto/User.java
│   ├── repository/UserRepository.java
│   ├── mapper/UserMapper.java
│   └── service/
│       ├── UserService.java
│       └── impl/UserServiceImpl.java
├── resources/
│   ├── sql/                                       ← SQL 2 类
│   │   ├── user_menu.sql
│   │   └── user_permission.sql
│   └── frontend/views/user/                       ← 前端 2 类
│       ├── index.vue                              ← 列表页
│       └── user-form.vue                          ← 表单弹窗
```

**生成顺序**：
1. 后端：Controller → Service → Entity → DTO → Repository → Mapper
2. SQL：Menu → Permission
3. 前端：Vue 列表页 → Vue 表单弹窗

> 与 `generate` 单类型生成不同，`gen-all` 的 `-f/--fields` 参数为**必填**，因为前端 Vue 页面和菜单 SQL 需要字段列表才能正确渲染。

### module

在当前项目中创建新的 Foundation 功能模块骨架，自动生成 `pom.xml`（继承 `aryee-foundation` 聚合 POM）和基础包目录。

```bash
java -jar foundation-cli.jar module -n notification
```

**生成的文件**：

```
aryee-foundation-notification/
├── pom.xml                                       ← parent 指向 aryee-foundation，artifactId=foundation-notification
└── src/main/java/cn/aryee/notification/
    └── package-info.java                         ← package cn.aryee.notification;
```

### check

校验当前项目是否符合 Foundation 标准结构。

```bash
# 基本校验
java -jar foundation-cli.jar check

# 严格模式（额外检查 README.md）
java -jar foundation-cli.jar check --strict

# 指定项目目录
java -jar foundation-cli.jar check -p /path/to/project
```

**参数**：

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `-p, --project` | 项目目录 | `.` |
| `--strict` | 启用严格校验（额外检查 `README.md`） | `false` |

**检查项**：

| 检查项 | 普通模式 | 严格模式 |
|--------|----------|----------|
| `pom.xml` 存在 | ✅ | ✅ |
| `src/main/java` 目录存在 | ✅ | ✅ |
| `src/main/resources` 存在 | WARN | WARN |
| `.gitignore` 存在 | WARN | WARN |
| Java 源文件数量统计 | INFO | INFO |
| `README.md` 存在 | ❌ | WARN |

输出示例：

```
Validating project structure: /Users/aryee/my-project
--------------------------------------------------
  [PASS] pom.xml exists
  [PASS] src/main/java exists
  [WARN] src/main/resources not found
  [PASS] .gitignore exists
  [INFO] Java files: 12
--------------------------------------------------
Result: 3 passed, 0 errors, 1 warnings

Project structure is valid.
```

### deps

扫描项目中所有 `pom.xml` 文件，统计依赖分布情况。

```bash
# 分析当前目录
java -jar foundation-cli.jar deps

# 指定项目目录
java -jar foundation-cli.jar deps -p /path/to/project

# 按 scope 过滤
java -jar foundation-cli.jar deps --scope test
```

**参数**：

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `-p, --project` | 项目目录 | `.` |
| `--scope` | 过滤 scope：`compile` / `test` / `provided` / `runtime` | 不限 |

**输出内容**：

1. **依赖分组分布** — 按 groupId 统计引用次数（按引用数降序）
2. **模块依赖数 TOP 10** — 依赖最多的 10 个模块
3. **汇总统计** — POM 文件总数、依赖引用总数

> 实现采用简单的 XML 行扫描（基于 `<dependency>` / `<groupId>` 标签），不解析完整 DOM，速度快但仅识别 POM 标准格式。

### crypto

配置加密工具，用于对配置文件中的敏感值进行加密/解密。基于 Jasypt（`PooledPBEStringEncryptor` + `PBEWithMD5AndTripleDES` 默认算法）。

```bash
# 生成 32 字节 Base64 密钥
java -jar foundation-cli.jar crypto generate-key -s 32

# 加密明文
java -jar foundation-cli.jar crypto encrypt \
  -k "my-secret-key" \
  -v "my-password" \
  -a "PBEWithMD5AndTripleDES"

# 解密密文（不带 ENC() 包装）
java -jar foundation-cli.jar crypto decrypt \
  -k "my-secret-key" \
  -v "encrypted-base64-value" \
  -a "PBEWithMD5AndTripleDES"
```

**子命令**：

| 子命令 | 说明 |
|------|------|
| `generate-key` | 生成指定长度的 Base64 密钥（`SecureRandom`） |
| `encrypt` | 加密明文值，输出 `ENC(密文)` 格式 |
| `decrypt` | 解密密文值（输入为不带 `ENC()` 包装的 Base64 字符串） |

**通用参数**：

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `-k, --key` | 加密密钥（`encrypt` / `decrypt` 必填） | - |
| `-v, --value` | 加密 / 解密的值 | - |
| `-a, --algorithm` | 加密算法 | `PBEWithMD5AndTripleDES` |
| `-s, --size` | 密钥长度（字节，仅 `generate-key`） | `32` |

**Jasypt 配置默认值**（`SimpleStringPBEConfig`）：
- `keyObtentionIterations`: 1000
- `poolSize`: 1
- `providerName`: SunJCE
- `saltGeneratorClassName`: `org.jasypt.salt.RandomSaltGenerator`
- `ivGeneratorClassName`: `org.jasypt.iv.RandomIvGenerator`
- `stringOutputType`: base64

**使用场景**：
- 配置文件中数据库密码加密：`password: ENC(xxxxxx)`
- API 密钥加密存储
- 多环境配置差异化加密

密钥通过环境变量 `ARYEE_CRYPTO_KEY` 传入应用，不硬编码在配置文件中。

---

## 技术栈

| 依赖 | 说明 |
|------|------|
| [Picocli](https://picocli.info/) | 命令行解析框架（`@Command` / `@Option`） |
| [FreeMarker](https://freemarker.apache.org/) | 代码模板引擎（`Configuration.VERSION_2_3_32`） |
| [Jasypt](http://www.jasypt.org/) | 配置加密（`PooledPBEStringEncryptor`） |
| [SnakeYAML](https://bitbucket.org/snakeyaml/snakeyaml/) | YAML 解析 |
| Spring Boot Starter | Spring 容器支持（CLI 内部使用） |
| commons-core / commons-domain / commons-spring | Foundation 公共能力 |

## 模块结构

```
aryee-foundation-cli/
├── pom.xml                                       ← maven-shade-plugin 配置，mainClass=cn.aryee.cli.FoundationCli
└── src/main/
    ├── java/cn/aryee/cli/
    │   ├── FoundationCli.java                    ← CLI 入口 + init/generate/gen-all/module 命令
    │   ├── CodeGenerator.java                    ← FreeMarker 代码生成器（支持后端/SQL/前端）
    │   ├── command/
    │   │   ├── VersionCommand.java               ← version 命令
    │   │   ├── ProjectValidator.java             ← check 命令
    │   │   ├── DependencyAnalyzer.java           ← deps 命令
    │   │   └── CryptoCommand.java                ← crypto 命令（含 Encrypt/Decrypt/GenerateKey 子命令）
    │   └── util/
    │       └── CliOutput.java                    ← 终端输出工具（ANSI 颜色 / 状态 / 表格）
    └── resources/
        └── templates/                            ← FreeMarker 模板（共 11 个）
            ├── controller.ftl                    ← 后端 Controller
            ├── service.ftl                       ← 后端 Service 接口
            ├── service-impl.ftl                  ← 后端 Service 实现
            ├── entity.ftl                        ← 后端 Entity
            ├── dto.ftl                           ← 后端 DTO
            ├── repository.ftl                    ← 后端 Repository
            ├── mapper.ftl                        ← 后端 Mapper
            ├── menu.ftl                          ← SQL：菜单+按钮
            ├── permission.ftl                    ← SQL：权限+数据权限
            ├── vue.ftl                           ← 前端：Vue3 列表页
            └── vue-form.ftl                      ← 前端：Vue3 表单弹窗
```

## 构建

### 前置条件

- JDK 21+
- Maven 3.9+

### 打包

```bash
mvn clean package -DskipTests
```

产物：`target/foundation-cli.jar`（Fat JAR，包含所有依赖）

### 直接运行（开发模式，不打包）

```bash
mvn compile exec:java -Dexec.mainClass="cn.aryee.cli.FoundationCli" -Dexec.args="version"
```

## 兼容性

| 环境 | 版本要求 |
|------|----------|
| JDK | 21+ |
| Maven | 3.9+ |
| Spring Boot | 4.0.7 |
| Picocli | 由 bom-base 统一管理 |
| FreeMarker | 由 bom-base 统一管理 |
| Jasypt | 由 bom-base 统一管理 |
