package cn.aryee.examples.database.oracle.reactive;

import cn.aryee.database.api.query.PageResult;
import cn.aryee.examples.database.oracle.reactive.entity.User;
import cn.aryee.examples.database.oracle.reactive.service.UserService;
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
import org.springframework.test.context.TestConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Aryee Database Reactive 模式 R2DBC + Oracle 集成测试
 * 验证 BaseR2dbcDataService 与真实 Oracle 23ai 服务的连通性和功能正确性
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootTest
@DisplayName("Database 集成测试 - Reactive 模式 (R2DBC + Oracle)")
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class R2dbcOracleIntegrationTest {

    private final UserService userService;

    private final DatabaseClient databaseClient;

    private static final String DROP_DDL = "DROP TABLE \"ARYEE_TEST_USER_R2DBC\"";

    private static final String CREATE_DDL = """
            CREATE TABLE "ARYEE_TEST_USER_R2DBC" (
                "id" NUMBER(20) NOT NULL,
                "username" VARCHAR2(64) NOT NULL,
                "email" VARCHAR2(128),
                "age" NUMBER(10),
                "status" NUMBER(10),
                "create_time" TIMESTAMP,
                "update_time" TIMESTAMP,
                "version" NUMBER(20),
                "deleted" NUMBER(1) DEFAULT 0,
                PRIMARY KEY ("id")
            )
            """;

    @BeforeAll
    static void createTable(@Autowired DatabaseClient databaseClient) {
        // Oracle 不支持 IF EXISTS，先尝试 DROP（忽略错误），再 CREATE
        Mono<Void> init = databaseClient.sql(DROP_DDL).fetch().rowsUpdated()
                .onErrorResume(e -> Mono.empty())
                .then(databaseClient.sql(CREATE_DDL).fetch().rowsUpdated()
                        .onErrorResume(e -> Mono.empty())
                        .then());
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
        databaseClient.sql("DELETE FROM \"ARYEE_TEST_USER_R2DBC\"").fetch().rowsUpdated().block();
    }

    @AfterEach
    void tearDown() {
        databaseClient.sql("DELETE FROM \"ARYEE_TEST_USER_R2DBC\"").fetch().rowsUpdated().block();
    }

    // ==================== 基本 CRUD ====================

    @Test
    @DisplayName("create - 创建用户（SnowflakeId 自动生成）")
    void testCreate() {
        User user = buildUser("ora-rx-tom", "ora-rx-tom@aryee.cn", 25);

        StepVerifier.create(userService.create(user))
                .assertNext(saved -> {
                    assertThat(saved.getId()).isNotNull();
                    assertThat(saved.getUsername()).isEqualTo("ora-rx-tom");
                    assertThat(saved.getCreateTime()).isNotNull();
                    assertThat(saved.getUpdateTime()).isNotNull();
                    assertThat(saved.getDeleted()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getById - 根据 ID 查询")
    void testGetById() {
        User user = buildUser("ora-rx-jerry", "ora-rx-jerry@aryee.cn", 28);

        StepVerifier.create(userService.create(user)
                        .flatMap(saved -> userService.getById(saved.getId())))
                .assertNext(result -> {
                    assertThat(result).isPresent();
                    assertThat(result.get().getUsername()).isEqualTo("ora-rx-jerry");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("deleteById - 根据 ID 删除")
    void testDeleteById() {
        StepVerifier.create(userService.create(buildUser("ora-rx-bob", "ora-rx-bob@aryee.cn", 30))
                        .flatMap(saved -> userService.deleteById(saved.getId())
                                .then(userService.existsById(saved.getId()))))
                .assertNext(exists -> assertThat(exists).isFalse())
                .verifyComplete();
    }

    @Test
    @DisplayName("getAll - 获取全部")
    void testGetAll() {
        Flux<User> setup = Flux.just(
                        buildUser("ora-rx-u1", "ora-rx-u1@aryee.cn", 20),
                        buildUser("ora-rx-u2", "ora-rx-u2@aryee.cn", 21),
                        buildUser("ora-rx-u3", "ora-rx-u3@aryee.cn", 22))
                .concatMap(userService::create);

        StepVerifier.create(setup.then(userService.getAll().collectList()))
                .assertNext(users -> assertThat(users).hasSize(3))
                .verifyComplete();
    }

    @Test
    @DisplayName("count - 统计数量")
    void testCount() {
        Flux<User> setup = Flux.just(
                        buildUser("ora-rx-c1", "ora-rx-c1@aryee.cn", 20),
                        buildUser("ora-rx-c2", "ora-rx-c2@aryee.cn", 21))
                .concatMap(userService::create);

        StepVerifier.create(setup.then(userService.count()))
                .assertNext(count -> assertThat(count).isEqualTo(2L))
                .verifyComplete();
    }

    @Test
    @DisplayName("existsById - 判断存在")
    void testExistsById() {
        StepVerifier.create(userService.create(buildUser("ora-rx-exist", "ora-rx-exist@aryee.cn", 25))
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
                buildUser("ora-rx-b1", "ora-rx-b1@aryee.cn", 20),
                buildUser("ora-rx-b2", "ora-rx-b2@aryee.cn", 21),
                buildUser("ora-rx-b3", "ora-rx-b3@aryee.cn", 22)
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
                buildUser("ora-rx-g1", "ora-rx-g1@aryee.cn", 20),
                buildUser("ora-rx-g2", "ora-rx-g2@aryee.cn", 21),
                buildUser("ora-rx-g3", "ora-rx-g3@aryee.cn", 22)
        );

        StepVerifier.create(userService.batchCreate(users)
                        .collectList()
                        .flatMapMany(saved -> userService.getByIds(
                                saved.stream().map(user -> user.getId()).limit(2).toList()))
                        .collectList())
                .assertNext(result -> assertThat(result).hasSize(2))
                .verifyComplete();
    }

    // ==================== 条件查询 ====================

    @Test
    @DisplayName("getByField - 根据字段查询")
    void testGetByField() {
        StepVerifier.create(userService.create(buildUser("ora-rx-field", "ora-rx-field@aryee.cn", 25))
                        .thenMany(userService.getByField("username", "ora-rx-field"))
                        .collectList())
                .assertNext(result -> {
                    assertThat(result).hasSize(1);
                    assertThat(result.get(0).getEmail()).isEqualTo("ora-rx-field@aryee.cn");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getOneByField - 根据字段查询单个")
    void testGetOneByField() {
        StepVerifier.create(userService.create(buildUser("ora-rx-one", "ora-rx-one@aryee.cn", 26))
                        .then(userService.getOneByField("username", "ora-rx-one")))
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
                .map(i -> buildUser("ora-rx-pp" + i, "ora-rx-pp" + i + "@aryee.cn", 20 + i))
                .concatMap(userService::create);

        StepVerifier.create(setup.then(userService.getPage(1, 3)))
                .assertNext(page -> {
                    assertThat(page.getContent()).hasSize(3);
                    assertThat(page.getTotalElements()).isEqualTo(7);
                    assertThat(page.getPageSize()).isEqualTo(3);
                })
                .verifyComplete();
    }

    // ==================== 软删除 ====================

    @Test
    @DisplayName("softDeleteById - 软删除（标记 deleted=true）")
    void testSoftDeleteById() {
        StepVerifier.create(userService.create(buildUser("ora-rx-soft-del", "ora-rx-sd@aryee.cn", 25))
                        .flatMap(saved -> userService.softDeleteById(saved.getId())
                                .then(userService.getById(saved.getId()))))
                .assertNext(result -> {
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
        Class<?> clazz = userService.getClass();
        String className = clazz.getSimpleName();
        assertThat(className).contains("UserService");
        Class<?> superclass = clazz.getSuperclass();
        String superClassName = superclass.getSimpleName();
        assertThat(superClassName).satisfiesAnyOf(
                name -> assertThat(name).isEqualTo("BaseR2dbcDataService"),
                name -> assertThat(name).isEqualTo("UserService")
        );
    }
}
