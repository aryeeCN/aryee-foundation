package cn.aryee.examples.database.blocking;

import cn.aryee.examples.database.blocking.entity.User;
import cn.aryee.examples.database.blocking.service.UserService;
import cn.aryee.commons.domain.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Aryee Database Blocking 模式 JPA + MySQL 集成测试
 * 验证 BaseJpaDataService 与真实 MySQL 服务的连通性和功能正确性
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional  // 每个测试方法事务回滚，避免污染数据库
@DisplayName("Database 集成测试 - Blocking 模式 (JPA + MySQL)")
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class JpaBlockingIntegrationTest {

    private final UserService userService;

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
        // 清空全部数据（deleteByCondition 传空 Map 表示删除全部）
        userService.deleteByCondition(new HashMap<>());
    }

    @AfterEach
    void tearDown() {
        userService.deleteByCondition(new HashMap<>());
    }

    // ==================== 基本 CRUD ====================

    @Test
    @DisplayName("create - 创建用户（SnowflakeId 自动生成）")
    void testCreate() {
        User user = buildUser("tom", "tom@aryee.cn", 25);
        User saved = userService.create(user);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUsername()).isEqualTo("tom");
        assertThat(saved.getDeleted()).isFalse();
    }

    @Test
    @DisplayName("getById - 根据 ID 查询")
    void testGetById() {
        User saved = userService.create(buildUser("jerry", "jerry@aryee.cn", 28));
        Optional<User> result = userService.getById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("jerry");
    }

    @Test
    @DisplayName("getByIdOrThrow - 查询不存在时抛异常")
    void testGetByIdOrThrow() {
        assertThatThrownBy(() -> userService.getByIdOrThrow(99999999L))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("update - 更新实体")
    void testUpdate() {
        User saved = userService.create(buildUser("alice", "alice@aryee.cn", 22));
        saved.setAge(23);
        saved.setEmail("alice-new@aryee.cn");
        User updated = userService.update(saved);

        assertThat(updated.getAge()).isEqualTo(23);
        assertThat(updated.getEmail()).isEqualTo("alice-new@aryee.cn");
    }

    @Test
    @DisplayName("deleteById - 根据 ID 删除")
    void testDeleteById() {
        User saved = userService.create(buildUser("bob", "bob@aryee.cn", 30));
        Long id = saved.getId();

        userService.deleteById(id);

        assertThat(userService.existsById(id)).isFalse();
    }

    @Test
    @DisplayName("getAll - 获取全部")
    void testGetAll() {
        userService.create(buildUser("u1", "u1@aryee.cn", 20));
        userService.create(buildUser("u2", "u2@aryee.cn", 21));
        userService.create(buildUser("u3", "u3@aryee.cn", 22));

        List<User> all = userService.getAll();
        assertThat(all).hasSize(3);
    }

    @Test
    @DisplayName("count - 统计数量")
    void testCount() {
        userService.create(buildUser("c1", "c1@aryee.cn", 20));
        userService.create(buildUser("c2", "c2@aryee.cn", 21));

        long count = userService.count();
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("existsById - 判断存在")
    void testExistsById() {
        User saved = userService.create(buildUser("exist", "exist@aryee.cn", 25));
        assertThat(userService.existsById(saved.getId())).isTrue();
        assertThat(userService.existsById(999999999L)).isFalse();
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

        List<User> created = userService.batchCreate(users);
        assertThat(created).hasSize(3);
        assertThat(userService.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("batchDeleteByIds - 批量删除")
    void testBatchDeleteByIds() {
        List<User> users = userService.batchCreate(List.of(
                buildUser("d1", "d1@aryee.cn", 20),
                buildUser("d2", "d2@aryee.cn", 21)
        ));

        List<Long> ids = users.stream().map(User::getId).toList();
        userService.batchDeleteByIds(ids);

        assertThat(userService.count()).isZero();
    }

    @Test
    @DisplayName("getByIds - 批量查询")
    void testGetByIds() {
        List<User> users = userService.batchCreate(List.of(
                buildUser("g1", "g1@aryee.cn", 20),
                buildUser("g2", "g2@aryee.cn", 21),
                buildUser("g3", "g3@aryee.cn", 22)
        ));

        List<Long> ids = users.stream().map(User::getId).limit(2).toList();
        List<User> result = userService.getByIds(ids);
        assertThat(result).hasSize(2);
    }

    // ==================== 条件查询 ====================

    @Test
    @DisplayName("getByField - 根据字段查询")
    void testGetByField() {
        userService.create(buildUser("field-test", "field@aryee.cn", 25));

        List<User> result = userService.getByField("username", "field-test");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("field@aryee.cn");
    }

    @Test
    @DisplayName("getOneByField - 根据字段查询单个")
    void testGetOneByField() {
        userService.create(buildUser("one-field", "one-field@aryee.cn", 26));

        Optional<User> result = userService.getOneByField("username", "one-field");
        assertThat(result).isPresent();
        assertThat(result.get().getAge()).isEqualTo(26);
    }

    @Test
    @DisplayName("getByCondition - 根据条件查询列表")
    void testGetByCondition() {
        userService.create(buildUser("cond1", "cond1@aryee.cn", 25));
        userService.create(buildUser("cond2", "cond2@aryee.cn", 25));
        userService.create(buildUser("cond3", "cond3@aryee.cn", 30));

        Map<String, Object> condition = new HashMap<>();
        condition.put("age", 25);

        List<User> result = userService.getByCondition(condition);
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("countByCondition - 根据条件统计")
    void testCountByCondition() {
        userService.create(buildUser("cc1", "cc1@aryee.cn", 25));
        userService.create(buildUser("cc2", "cc2@aryee.cn", 25));
        userService.create(buildUser("cc3", "cc3@aryee.cn", 30));

        Map<String, Object> condition = new HashMap<>();
        condition.put("age", 25);

        long count = userService.countByCondition(condition);
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("existsByCondition - 条件存在性检查")
    void testExistsByCondition() {
        userService.create(buildUser("exists-cond", "ec@aryee.cn", 27));

        Map<String, Object> condition = new HashMap<>();
        condition.put("username", "exists-cond");
        assertThat(userService.existsByCondition(condition)).isTrue();

        condition.clear();
        condition.put("username", "not-exists");
        assertThat(userService.existsByCondition(condition)).isFalse();
    }

    // ==================== 分页查询 ====================

    @Test
    @DisplayName("getPage(Pageable) - Spring Data Page 分页")
    void testGetPageWithPageable() {
        for (int i = 1; i <= 5; i++) {
            userService.create(buildUser("p" + i, "p" + i + "@aryee.cn", 20 + i));
        }

        Page<User> page = userService.getPage(PageRequest.of(0, 3));
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("getPage(int, int) - 统一 PageResult 分页")
    void testGetPageWithPageNum() {
        for (int i = 1; i <= 7; i++) {
            userService.create(buildUser("pp" + i, "pp" + i + "@aryee.cn", 20 + i));
        }

        PageResult<User> page = userService.getPage(1, 3);
        assertThat(page.getRecords()).hasSize(3);
        assertThat(page.getTotal()).isEqualTo(7);
        assertThat(page.getPageNum()).isEqualTo(1);
        assertThat(page.getPageSize()).isEqualTo(3);
    }

    @Test
    @DisplayName("getPageByCondition - 条件分页查询")
    void testGetPageByCondition() {
        for (int i = 1; i <= 6; i++) {
            userService.create(buildUser("cp" + i, "cp" + i + "@aryee.cn", 25));
        }
        for (int i = 1; i <= 3; i++) {
            userService.create(buildUser("other" + i, "other" + i + "@aryee.cn", 30));
        }

        Map<String, Object> condition = new HashMap<>();
        condition.put("age", 25);

        PageResult<User> page = userService.getPageByCondition(condition, 1, 4);
        assertThat(page.getRecords()).hasSize(4);
        assertThat(page.getTotal()).isEqualTo(6);
    }

    // ==================== 软删除 ====================

    @Test
    @DisplayName("softDeleteById - 软删除（标记 deleted=true）")
    void testSoftDeleteById() {
        User saved = userService.create(buildUser("soft-del", "sd@aryee.cn", 25));
        Long id = saved.getId();

        userService.softDeleteById(id);

        // 软删除将 deleted 标记为 true（getById 仍可查到，但 deleted=true）
        Optional<User> result = userService.getById(id);
        assertThat(result).isPresent();
        assertThat(result.get().getDeleted()).isTrue();
    }

    @Test
    @DisplayName("findDistinctValues - 字段去重查询")
    void testFindDistinctValues() {
        userService.create(buildUser("dv1", "dv1@aryee.cn", 25));
        userService.create(buildUser("dv2", "dv2@aryee.cn", 25));
        userService.create(buildUser("dv3", "dv3@aryee.cn", 30));

        List<Integer> ages = userService.findDistinctValues("age");
        assertThat(ages).contains(25, 30);
    }

    // ==================== 注入验证 ====================

    @Test
    @DisplayName("UserService 注入验证 - 确认使用 BaseJpaDataService 实现")
    void testServiceInjection() {
        assertThat(userService).isNotNull();
        // CGLIB 代理后类名带 $$SpringCGLIB$$ 后缀，用 contains 判断
        String className = userService.getClass().getSimpleName();
        assertThat(className).contains("UserService");
        // 父类应该是 BaseJpaDataService
        assertThat(userService.getClass().getSuperclass().getSimpleName())
                .isEqualTo("UserService");
        assertThat(userService.getClass().getSuperclass().getSuperclass().getSimpleName())
                .isEqualTo("BaseJpaDataService");
    }
}
