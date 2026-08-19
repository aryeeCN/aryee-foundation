package cn.aryee.examples.database.oracle.blocking;

import cn.aryee.commons.domain.model.PageResult;
import cn.aryee.examples.database.oracle.blocking.entity.JdbcUser;
import cn.aryee.examples.database.oracle.blocking.service.JdbcUserService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Aryee Database Blocking 模式 JDBC (MyBatis-Plus) + Oracle 集成测试
 * 验证 BaseJdbcDataService 与真实 Oracle 23ai 服务的连通性和功能正确性
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootTest
@ActiveProfiles("jdbc")
@DisplayName("Database 集成测试 - Blocking 模式 (JDBC/MyBatis-Plus + Oracle)")
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class JdbcOracleIntegrationTest {

    private final JdbcUserService jdbcUserService;

    private final JdbcTemplate jdbcTemplate;

    private JdbcUser buildUser(String username, String email, Integer age) {
        JdbcUser user = new JdbcUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setAge(age);
        user.setStatus(1);
        return user;
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM aryee_test_user");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM aryee_test_user");
    }

    // ==================== 基本 CRUD ====================

    @Test
    @DisplayName("create - 创建用户（SnowflakeId 自动生成）")
    void testCreate() {
        JdbcUser user = buildUser("ora-tom", "ora-tom@aryee.cn", 25);
        JdbcUser saved = jdbcUserService.create(user);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUsername()).isEqualTo("ora-tom");
        assertThat(saved.getDeleted()).isFalse();
    }

    @Test
    @DisplayName("getById - 根据 ID 查询")
    void testGetById() {
        JdbcUser saved = jdbcUserService.create(buildUser("ora-jerry", "ora-jerry@aryee.cn", 28));
        Optional<JdbcUser> result = jdbcUserService.getById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("ora-jerry");
    }

    @Test
    @DisplayName("getByIdOrThrow - 查询不存在时抛异常")
    void testGetByIdOrThrow() {
        assertThatThrownBy(() -> jdbcUserService.getByIdOrThrow(99999999L))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("update - 更新实体")
    void testUpdate() {
        JdbcUser saved = jdbcUserService.create(buildUser("ora-alice", "ora-alice@aryee.cn", 22));
        saved.setAge(23);
        saved.setEmail("ora-alice-new@aryee.cn");
        JdbcUser updated = jdbcUserService.update(saved);

        assertThat(updated.getAge()).isEqualTo(23);
        assertThat(updated.getEmail()).isEqualTo("ora-alice-new@aryee.cn");
    }

    @Test
    @DisplayName("deleteById - 根据 ID 删除")
    void testDeleteById() {
        JdbcUser saved = jdbcUserService.create(buildUser("ora-bob", "ora-bob@aryee.cn", 30));
        Long id = saved.getId();

        jdbcUserService.deleteById(id);

        assertThat(jdbcUserService.existsById(id)).isFalse();
    }

    @Test
    @DisplayName("getAll - 获取全部")
    void testGetAll() {
        jdbcUserService.create(buildUser("ou1", "ou1@aryee.cn", 20));
        jdbcUserService.create(buildUser("ou2", "ou2@aryee.cn", 21));
        jdbcUserService.create(buildUser("ou3", "ou3@aryee.cn", 22));

        List<JdbcUser> all = jdbcUserService.getAll();
        assertThat(all).hasSize(3);
    }

    @Test
    @DisplayName("count - 统计数量")
    void testCount() {
        jdbcUserService.create(buildUser("oc1", "oc1@aryee.cn", 20));
        jdbcUserService.create(buildUser("oc2", "oc2@aryee.cn", 21));

        long count = jdbcUserService.count();
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("existsById - 判断存在")
    void testExistsById() {
        JdbcUser saved = jdbcUserService.create(buildUser("ora-exist", "ora-exist@aryee.cn", 25));
        assertThat(jdbcUserService.existsById(saved.getId())).isTrue();
        assertThat(jdbcUserService.existsById(999999999L)).isFalse();
    }

    // ==================== 批量操作 ====================

    @Test
    @DisplayName("batchCreate - 批量创建")
    void testBatchCreate() {
        List<JdbcUser> users = List.of(
                buildUser("ob1", "ob1@aryee.cn", 20),
                buildUser("ob2", "ob2@aryee.cn", 21),
                buildUser("ob3", "ob3@aryee.cn", 22)
        );

        List<JdbcUser> created = jdbcUserService.batchCreate(users);
        assertThat(created).hasSize(3);
        assertThat(jdbcUserService.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("batchDeleteByIds - 批量删除")
    void testBatchDeleteByIds() {
        List<JdbcUser> users = jdbcUserService.batchCreate(List.of(
                buildUser("od1", "od1@aryee.cn", 20),
                buildUser("od2", "od2@aryee.cn", 21)
        ));

        List<Long> ids = users.stream().map(JdbcUser::getId).toList();
        jdbcUserService.batchDeleteByIds(ids);

        assertThat(jdbcUserService.count()).isZero();
    }

    @Test
    @DisplayName("getByIds - 批量查询")
    void testGetByIds() {
        List<JdbcUser> users = jdbcUserService.batchCreate(List.of(
                buildUser("og1", "og1@aryee.cn", 20),
                buildUser("og2", "og2@aryee.cn", 21),
                buildUser("og3", "og3@aryee.cn", 22)
        ));

        List<Long> ids = users.stream().map(JdbcUser::getId).limit(2).toList();
        List<JdbcUser> result = jdbcUserService.getByIds(ids);
        assertThat(result).hasSize(2);
    }

    // ==================== 条件查询 ====================

    @Test
    @DisplayName("getByField - 根据字段查询")
    void testGetByField() {
        jdbcUserService.create(buildUser("ora-field", "ora-field@aryee.cn", 25));

        List<JdbcUser> result = jdbcUserService.getByField("username", "ora-field");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("ora-field@aryee.cn");
    }

    @Test
    @DisplayName("getOneByField - 根据字段查询单个")
    void testGetOneByField() {
        jdbcUserService.create(buildUser("ora-one", "ora-one@aryee.cn", 26));

        Optional<JdbcUser> result = jdbcUserService.getOneByField("username", "ora-one");
        assertThat(result).isPresent();
        assertThat(result.get().getAge()).isEqualTo(26);
    }

    @Test
    @DisplayName("getByCondition - 根据条件查询列表")
    void testGetByCondition() {
        jdbcUserService.create(buildUser("ocond1", "ocond1@aryee.cn", 25));
        jdbcUserService.create(buildUser("ocond2", "ocond2@aryee.cn", 25));
        jdbcUserService.create(buildUser("ocond3", "ocond3@aryee.cn", 30));

        Map<String, Object> condition = new HashMap<>();
        condition.put("age", 25);

        List<JdbcUser> result = jdbcUserService.getByCondition(condition);
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("countByCondition - 根据条件统计")
    void testCountByCondition() {
        jdbcUserService.create(buildUser("occ1", "occ1@aryee.cn", 25));
        jdbcUserService.create(buildUser("occ2", "occ2@aryee.cn", 25));
        jdbcUserService.create(buildUser("occ3", "occ3@aryee.cn", 30));

        Map<String, Object> condition = new HashMap<>();
        condition.put("age", 25);

        long count = jdbcUserService.countByCondition(condition);
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("existsByCondition - 条件存在性检查")
    void testExistsByCondition() {
        jdbcUserService.create(buildUser("ora-exists-cond", "oec@aryee.cn", 27));

        Map<String, Object> condition = new HashMap<>();
        condition.put("username", "ora-exists-cond");
        assertThat(jdbcUserService.existsByCondition(condition)).isTrue();

        condition.clear();
        condition.put("username", "not-exists");
        assertThat(jdbcUserService.existsByCondition(condition)).isFalse();
    }

    // ==================== 分页查询 ====================

    @Test
    @DisplayName("getPage(int, int) - 统一 PageResult 分页")
    void testGetPageWithPageNum() {
        for (int i = 1; i <= 7; i++) {
            jdbcUserService.create(buildUser("opp" + i, "opp" + i + "@aryee.cn", 20 + i));
        }

        PageResult<JdbcUser> page = jdbcUserService.getPage(1, 3);
        assertThat(page.getRecords()).hasSize(3);
        assertThat(page.getTotal()).isEqualTo(7);
        assertThat(page.getPageNum()).isEqualTo(1);
        assertThat(page.getPageSize()).isEqualTo(3);
    }

    @Test
    @DisplayName("getPageByCondition - 条件分页查询")
    void testGetPageByCondition() {
        for (int i = 1; i <= 6; i++) {
            jdbcUserService.create(buildUser("ocp" + i, "ocp" + i + "@aryee.cn", 25));
        }
        for (int i = 1; i <= 3; i++) {
            jdbcUserService.create(buildUser("oother" + i, "oother" + i + "@aryee.cn", 30));
        }

        Map<String, Object> condition = new HashMap<>();
        condition.put("age", 25);

        PageResult<JdbcUser> page = jdbcUserService.getPageByCondition(condition, 1, 4);
        assertThat(page.getRecords()).hasSize(4);
        assertThat(page.getTotal()).isEqualTo(6);
    }

    // ==================== 软删除 ====================

    @Test
    @DisplayName("softDeleteById - 软删除（标记 deleted=true）")
    void testSoftDeleteById() {
        JdbcUser saved = jdbcUserService.create(buildUser("ora-soft-del", "osd@aryee.cn", 25));
        Long id = saved.getId();

        jdbcUserService.softDeleteById(id);

        // 使用原生 SQL 验证软删除结果（因为 MyBatis-Plus 逻辑删除会过滤已删除记录）
        Integer deletedFlag = jdbcTemplate.queryForObject(
                "SELECT deleted FROM aryee_test_user WHERE id = ?",
                Integer.class, id
        );
        assertThat(deletedFlag).isEqualTo(1);

        // 确认通过 MyBatis-Plus 查询不到已删除的记录
        Optional<JdbcUser> result = jdbcUserService.getById(id);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findDistinctValues - 字段去重查询")
    void testFindDistinctValues() {
        jdbcUserService.create(buildUser("odv1", "odv1@aryee.cn", 25));
        jdbcUserService.create(buildUser("odv2", "odv2@aryee.cn", 25));
        jdbcUserService.create(buildUser("odv3", "odv3@aryee.cn", 30));

        // Oracle NUMBER 类型通过 JDBC 返回 BigDecimal，需要转为 int 比较
        List<?> ages = jdbcUserService.findDistinctValues("age");
        assertThat(ages).extracting(e -> ((Number) e).intValue()).contains(25, 30);
    }

    // ==================== 注入验证 ====================

    @Test
    @DisplayName("JdbcUserService 注入验证")
    void testServiceInjection() {
        assertThat(jdbcUserService).isNotNull();
        String className = jdbcUserService.getClass().getSimpleName();
        assertThat(className).contains("JdbcUserService");
    }
}
