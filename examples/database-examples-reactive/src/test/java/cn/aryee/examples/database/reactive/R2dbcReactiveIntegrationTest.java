package cn.aryee.examples.database.reactive;

import cn.aryee.examples.database.reactive.entity.User;
import cn.aryee.examples.database.reactive.service.UserService;
import cn.aryee.commons.domain.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Aryee Database Reactive 模式 R2DBC + MySQL 集成测试
 * 验证 BaseR2dbcDataService 与真实 MySQL 服务的连通性和功能正确性
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Database 集成测试 - Reactive 模式 (R2DBC + MySQL)")
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class R2dbcReactiveIntegrationTest {

    private final UserService userService;

    private final DatabaseClient databaseClient;

    private static final String DROP_DDL =
            "DROP TABLE IF EXISTS aryee_test_user_r2dbc";

    private static final String CREATE_DDL = """
            CREATE TABLE aryee_test_user_r2dbc (
                id BIGINT NOT NULL PRIMARY KEY COMMENT '主键ID',
                username VARCHAR(64) NOT NULL COMMENT '用户名',
                email VARCHAR(128) COMMENT '邮箱',
                age INT COMMENT '年龄',
                status INT COMMENT '状态',
                create_time DATETIME COMMENT '创建时间',
                update_time DATETIME COMMENT '更新时间',
                version BIGINT COMMENT '乐观锁版本号',
                deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标记'
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Aryee 测试用户表'
            """;

    @BeforeAll
    static void createTable(@Autowired DatabaseClient databaseClient) {
        // r2dbc-mysql 驱动对 DDL 的返回值处理有 Bug，需要 blockLast 同步等待
        Mono<Long> init = databaseClient.sql(DROP_DDL).fetch().rowsUpdated()
                .onErrorResume(e -> Mono.empty())  // 忽略 drop 错误
                .then(databaseClient.sql(CREATE_DDL).fetch().rowsUpdated()
                        .onErrorResume(e -> Mono.empty()));
        init.block();
    }

    @AfterAll
    static void dropTable(@Autowired DatabaseClient databaseClient) {
        databaseClient.sql(DROP_DDL).fetch().rowsUpdated()
                .onErrorResume(e -> Mono.empty())
                .block();
    }

    private User buildUser(String username, String email, Integer age) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setAge(age);
        user.setStatus(1);
        return user;
    }

    @BeforeEach
    void setUp() {
        // 清空全部数据
        databaseClient.sql("DELETE FROM aryee_test_user_r2dbc").fetch().rowsUpdated().block();
    }

    @AfterEach
    void tearDown() {
        databaseClient.sql("DELETE FROM aryee_test_user_r2dbc").fetch().rowsUpdated().block();
    }

    // ==================== 基本 CRUD ====================

    @Test
    @DisplayName("create - 创建用户（SnowflakeId 自动生成）")
    void testCreate() {
        User user = buildUser("tom", "tom@aryee.cn", 25);

        StepVerifier.create(userService.create(user))
                .assertNext(saved -> {
                    assertThat(saved.getId()).isNotNull();
                    assertThat(saved.getUsername()).isEqualTo("tom");
                    assertThat(saved.getCreateTime()).isNotNull();
                    assertThat(saved.getUpdateTime()).isNotNull();
                    assertThat(saved.getDeleted()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getById - 根据 ID 查询")
    void testGetById() {
        User user = buildUser("jerry", "jerry@aryee.cn", 28);

        StepVerifier.create(userService.create(user)
                        .flatMap(saved -> userService.getById(saved.getId())))
                .assertNext(result -> {
                    assertThat(result).isPresent();
                    assertThat(result.get().getUsername()).isEqualTo("jerry");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("deleteById - 根据 ID 删除")
    void testDeleteById() {
        StepVerifier.create(userService.create(buildUser("bob", "bob@aryee.cn", 30))
                        .flatMap(saved -> userService.deleteById(saved.getId())
                                .then(userService.existsById(saved.getId()))))
                .assertNext(exists -> assertThat(exists).isFalse())
                .verifyComplete();
    }

    @Test
    @DisplayName("getAll - 获取全部")
    void testGetAll() {
        Flux<User> setup = Flux.just(
                        buildUser("u1", "u1@aryee.cn", 20),
                        buildUser("u2", "u2@aryee.cn", 21),
                        buildUser("u3", "u3@aryee.cn", 22))
                .concatMap(userService::create);

        StepVerifier.create(setup.then(userService.getAll().collectList()))
                .assertNext(users -> assertThat(users).hasSize(3))
                .verifyComplete();
    }

    @Test
    @DisplayName("count - 统计数量")
    void testCount() {
        Flux<User> setup = Flux.just(
                        buildUser("c1", "c1@aryee.cn", 20),
                        buildUser("c2", "c2@aryee.cn", 21))
                .concatMap(userService::create);

        StepVerifier.create(setup.then(userService.count()))
                .assertNext(count -> assertThat(count).isEqualTo(2L))
                .verifyComplete();
    }

    @Test
    @DisplayName("existsById - 判断存在")
    void testExistsById() {
        StepVerifier.create(userService.create(buildUser("exist", "exist@aryee.cn", 25))
                        .flatMap(saved -> userService.existsById(saved.getId())))
                .assertNext(exists -> assertThat(exists).isTrue())
                .verifyComplete();

        StepVerifier.create(userService.existsById(999999999L))
                .assertNext(exists -> assertThat(exists).isFalse())
                .verifyComplete();
    }

    // ==================== 批量操作 ====================

    @Test
    @DisplayName("batchCreate - 批量创建")
    void testBatchCreate() {
        List<User> users = List.of(
                buildUser("b1", "b1@aryee.cn", 20),
                buildUser("b2", "b2@aryee.cn", 21),
                buildUser("b3", "b3@aryee.cn", 22)
        );

        StepVerifier.create(userService.batchCreate(users).collectList())
                .assertNext(created -> {
                    assertThat(created).hasSize(3);
                    created.forEach(u -> assertThat(u.getId()).isNotNull());
                })
                .verifyComplete();

        StepVerifier.create(userService.count())
                .assertNext(count -> assertThat(count).isEqualTo(3L))
                .verifyComplete();
    }

    @Test
    @DisplayName("getByIds - 批量查询")
    void testGetByIds() {
        List<User> users = List.of(
                buildUser("g1", "g1@aryee.cn", 20),
                buildUser("g2", "g2@aryee.cn", 21),
                buildUser("g3", "g3@aryee.cn", 22)
        );

        StepVerifier.create(userService.batchCreate(users)
                        .collectList()
                        .flatMapMany(saved -> userService.getByIds(
                                saved.stream().map(User::getId).limit(2).toList()))
                        .collectList())
                .assertNext(result -> assertThat(result).hasSize(2))
                .verifyComplete();
    }

    // ==================== 条件查询 ====================

    @Test
    @DisplayName("getByField - 根据字段查询")
    void testGetByField() {
        StepVerifier.create(userService.create(buildUser("field-test", "field@aryee.cn", 25))
                        .thenMany(userService.getByField("username", "field-test"))
                        .collectList())
                .assertNext(result -> {
                    assertThat(result).hasSize(1);
                    assertThat(result.get(0).getEmail()).isEqualTo("field@aryee.cn");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getOneByField - 根据字段查询单个")
    void testGetOneByField() {
        StepVerifier.create(userService.create(buildUser("one-field", "one-field@aryee.cn", 26))
                        .then(userService.getOneByField("username", "one-field")))
                .assertNext(result -> {
                    assertThat(result).isPresent();
                    assertThat(result.get().getAge()).isEqualTo(26);
                })
                .verifyComplete();
    }

    // ==================== 分页查询 ====================

    @Test
    @DisplayName("getPage(int, int) - 统一 PageResult 分页")
    void testGetPageWithPageNum() {
        Flux<User> setup = Flux.range(1, 7)
                .map(i -> buildUser("pp" + i, "pp" + i + "@aryee.cn", 20 + i))
                .concatMap(userService::create);

        StepVerifier.create(setup.then(userService.getPage(1, 3)))
                .assertNext(page -> {
                    assertThat(page.getRecords()).hasSize(3);
                    assertThat(page.getTotal()).isEqualTo(7);
                    assertThat(page.getPageSize()).isEqualTo(3);
                })
                .verifyComplete();
    }

    // ==================== 软删除 ====================

    @Test
    @DisplayName("softDeleteById - 软删除（标记 deleted=true）")
    void testSoftDeleteById() {
        StepVerifier.create(userService.create(buildUser("soft-del", "sd@aryee.cn", 25))
                        .flatMap(saved -> userService.softDeleteById(saved.getId())
                                .then(userService.getById(saved.getId()))))
                .assertNext(result -> {
                    // 软删除将 deleted 标记为 true
                    assertThat(result).isPresent();
                    assertThat(result.get().getDeleted()).isTrue();
                })
                .verifyComplete();
    }

    // ==================== 注入验证 ====================

    @Test
    @DisplayName("UserService 注入验证 - 确认使用 BaseR2dbcDataService 实现")
    void testServiceInjection() {
        assertThat(userService).isNotNull();
        // R2DBC 场景下 UserService 通常不被 CGLIB 代理（无 @Transactional），直接是原类
        Class<?> clazz = userService.getClass();
        String className = clazz.getSimpleName();
        // 可能是 UserService 或 UserService$$SpringCGLIB$$0
        assertThat(className).contains("UserService");
        // 父类应该是 BaseR2dbcDataService（无代理）或 UserService（有代理时父类是 UserService）
        Class<?> superclass = clazz.getSuperclass();
        String superClassName = superclass.getSimpleName();
        assertThat(superClassName).satisfiesAnyOf(
                name -> assertThat(name).isEqualTo("BaseR2dbcDataService"),
                name -> assertThat(name).isEqualTo("UserService")
        );
    }
}
