package cn.aryee.examples.tenant.blocking;

import cn.aryee.tenant.api.annotation.TenantSwitch;
import cn.aryee.tenant.api.context.TenantContextHolder;
import cn.aryee.tenant.api.enums.TenantStatus;
import cn.aryee.tenant.api.enums.TenantType;
import cn.aryee.tenant.api.model.Tenant;
import cn.aryee.tenant.api.service.TenantService;
import cn.aryee.tenant.api.service.TenantValidator;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.test.context.TestConstructor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Aryee Tenant Blocking 模式集成测试
 * 验证 DefaultTenantService、DefaultTenantValidator、TenantSwitch 等核心功能
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootTest
@Import(TenantBlockingIntegrationTest.TenantSwitchTestService.class)
@DisplayName("Tenant 多租户集成测试 - Blocking 模式")
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class TenantBlockingIntegrationTest {

    private final TenantService tenantService;

    private final TenantValidator tenantValidator;

    private final TenantSwitchTestService tenantSwitchTestService;

    @BeforeEach
    void setUp() {
        TenantContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // ========== TenantService CRUD ==========

    @Test
    @DisplayName("createTenant + getTenantById - 创建与获取租户")
    void testCreateAndGetTenant() {
        Tenant tenant = new Tenant()
                .setTenantCode("T001")
                .setTenantName("测试租户001")
                .setTenantType(TenantType.ENTERPRISE)
                .setQuota(1000L);

        Tenant created = tenantService.createTenant(tenant);

        assertThat(created).isNotNull();
        assertThat(created.getTenantId()).isNotNull().isNotEmpty();
        assertThat(created.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(created.getUsedQuota()).isEqualTo(0L);
        assertThat(created.getCreateTime()).isNotNull();
        assertThat(created.getUpdateTime()).isNotNull();

        Tenant fetched = tenantService.getTenantById(created.getTenantId());
        assertThat(fetched).isNotNull();
        assertThat(fetched.getTenantCode()).isEqualTo("T001");
        assertThat(fetched.getTenantName()).isEqualTo("测试租户001");
        assertThat(fetched.getTenantType()).isEqualTo(TenantType.ENTERPRISE);
    }

    @Test
    @DisplayName("createTenant - 自动生成租户ID")
    void testCreateTenantAutoGenerateId() {
        Tenant tenant = new Tenant()
                .setTenantCode("T-AUTO")
                .setTenantName("自动ID租户");

        Tenant created = tenantService.createTenant(tenant);

        assertThat(created.getTenantId()).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("getTenantByCode - 根据编码获取租户")
    void testGetTenantByCode() {
        Tenant tenant = new Tenant()
                .setTenantCode("T-CODE")
                .setTenantName("编码查询租户");
        tenantService.createTenant(tenant);

        Tenant fetched = tenantService.getTenantByCode("T-CODE");
        assertThat(fetched).isNotNull();
        assertThat(fetched.getTenantName()).isEqualTo("编码查询租户");
    }

    @Test
    @DisplayName("getTenantByCode - 不存在返回null")
    void testGetTenantByCodeNotFound() {
        Tenant fetched = tenantService.getTenantByCode("NONEXISTENT");
        assertThat(fetched).isNull();
    }

    @Test
    @DisplayName("updateTenant - 更新租户信息")
    void testUpdateTenant() {
        Tenant tenant = new Tenant()
                .setTenantCode("T-UPD")
                .setTenantName("原始名称");
        Tenant created = tenantService.createTenant(tenant);

        created.setTenantName("更新后名称");
        Tenant updated = tenantService.updateTenant(created);

        assertThat(updated.getTenantName()).isEqualTo("更新后名称");
        assertThat(updated.getCreateTime()).isNotNull();
        assertThat(updated.getUpdateTime()).isNotNull();
    }

    @Test
    @DisplayName("deleteTenant - 删除租户")
    void testDeleteTenant() {
        Tenant tenant = new Tenant()
                .setTenantCode("T-DEL")
                .setTenantName("待删除租户");
        Tenant created = tenantService.createTenant(tenant);

        assertThat(tenantService.getTenantById(created.getTenantId())).isNotNull();

        tenantService.deleteTenant(created.getTenantId());

        assertThat(tenantService.getTenantById(created.getTenantId())).isNull();
    }

    @Test
    @DisplayName("listAllTenants - 获取所有租户")
    void testListAllTenants() {
        tenantService.createTenant(new Tenant().setTenantCode("T-L1").setTenantName("列表1"));
        tenantService.createTenant(new Tenant().setTenantCode("T-L2").setTenantName("列表2"));
        tenantService.createTenant(new Tenant().setTenantCode("T-L3").setTenantName("列表3"));

        List<Tenant> tenants = tenantService.listAllTenants();
        assertThat(tenants).isNotEmpty();
    }

    @Test
    @DisplayName("listTenantsByStatus - 按状态查询租户")
    void testListTenantsByStatus() {
        tenantService.createTenant(new Tenant().setTenantCode("T-S1").setTenantName("活跃租户1"));
        Tenant inactive = tenantService.createTenant(
                new Tenant().setTenantCode("T-S2").setTenantName("停用租户"));
        tenantService.deactivateTenant(inactive.getTenantId());

        List<Tenant> activeTenants = tenantService.listTenantsByStatus(TenantStatus.ACTIVE);
        assertThat(activeTenants).anyMatch(t -> "T-S1".equals(t.getTenantCode()));

        List<Tenant> inactiveTenants = tenantService.listTenantsByStatus(TenantStatus.INACTIVE);
        assertThat(inactiveTenants).anyMatch(t -> "T-S2".equals(t.getTenantCode()));
    }

    @Test
    @DisplayName("listChildTenants - 子租户列表")
    void testListChildTenants() {
        Tenant parent = tenantService.createTenant(
                new Tenant().setTenantCode("T-PARENT").setTenantName("父租户"));

        tenantService.createTenant(
                new Tenant().setTenantCode("T- Child1")
                        .setTenantName("子租户1")
                        .setParentTenantId(parent.getTenantId())
                        .setTenantLevel(1));
        tenantService.createTenant(
                new Tenant().setTenantCode("T-Child2")
                        .setTenantName("子租户2")
                        .setParentTenantId(parent.getTenantId())
                        .setTenantLevel(1));

        List<Tenant> children = tenantService.listChildTenants(parent.getTenantId());
        assertThat(children).hasSizeGreaterThanOrEqualTo(2);
    }

    // ========== 租户生命周期 ==========

    @Test
    @DisplayName("activateTenant - 激活租户")
    void testActivateTenant() {
        Tenant tenant = tenantService.createTenant(
                new Tenant().setTenantCode("T-ACT").setTenantName("待激活租户"));
        tenantService.deactivateTenant(tenant.getTenantId());

        Tenant activated = tenantService.activateTenant(tenant.getTenantId());
        assertThat(activated.getStatus()).isEqualTo(TenantStatus.ACTIVE);
    }

    @Test
    @DisplayName("deactivateTenant - 停用租户")
    void testDeactivateTenant() {
        Tenant tenant = tenantService.createTenant(
                new Tenant().setTenantCode("T-DEACT").setTenantName("待停用租户"));

        Tenant deactivated = tenantService.deactivateTenant(tenant.getTenantId());
        assertThat(deactivated.getStatus()).isEqualTo(TenantStatus.INACTIVE);
    }

    @Test
    @DisplayName("suspendTenant - 挂起租户")
    void testSuspendTenant() {
        Tenant tenant = tenantService.createTenant(
                new Tenant().setTenantCode("T-SUS").setTenantName("待挂起租户"));

        Tenant suspended = tenantService.suspendTenant(tenant.getTenantId());
        assertThat(suspended.getStatus()).isEqualTo(TenantStatus.SUSPENDED);
    }

    @Test
    @DisplayName("cancelTenant - 注销租户")
    void testCancelTenant() {
        Tenant tenant = tenantService.createTenant(
                new Tenant().setTenantCode("T-CAN").setTenantName("待注销租户"));

        tenantService.cancelTenant(tenant.getTenantId());

        Tenant cancelled = tenantService.getTenantById(tenant.getTenantId());
        assertThat(cancelled.getStatus()).isEqualTo(TenantStatus.CANCELLED);
    }

    // ========== 配额管理 ==========

    @Test
    @DisplayName("checkQuota - 检查配额")
    void testCheckQuota() {
        Tenant tenant = tenantService.createTenant(
                new Tenant().setTenantCode("T-QUOTA")
                        .setTenantName("配额租户")
                        .setQuota(100L)
                        .setUsedQuota(30L));

        assertThat(tenantService.checkQuota(tenant.getTenantId(), 50L)).isTrue();
        assertThat(tenantService.checkQuota(tenant.getTenantId(), 80L)).isFalse();
    }

    @Test
    @DisplayName("increaseUsedQuota / decreaseUsedQuota - 配额增减")
    void testQuotaAdjustment() {
        Tenant tenant = tenantService.createTenant(
                new Tenant().setTenantCode("T-QA")
                        .setTenantName("配额调整租户")
                        .setQuota(200L)
                        .setUsedQuota(50L));

        tenantService.increaseUsedQuota(tenant.getTenantId(), 30L);
        Tenant increased = tenantService.getTenantById(tenant.getTenantId());
        assertThat(increased.getUsedQuota()).isEqualTo(80L);

        tenantService.decreaseUsedQuota(tenant.getTenantId(), 20L);
        Tenant decreased = tenantService.getTenantById(tenant.getTenantId());
        assertThat(decreased.getUsedQuota()).isEqualTo(60L);
    }

    @Test
    @DisplayName("updateQuota - 更新配额总量")
    void testUpdateQuota() {
        Tenant tenant = tenantService.createTenant(
                new Tenant().setTenantCode("T-UQ")
                        .setTenantName("配额更新租户")
                        .setQuota(100L));

        Tenant updated = tenantService.updateQuota(tenant.getTenantId(), 500L);
        assertThat(updated.getQuota()).isEqualTo(500L);
    }

    // ========== 租户校验 ==========

    @Test
    @DisplayName("validateTenant - 校验有效租户")
    void testValidateTenantValid() {
        Tenant tenant = tenantService.createTenant(
                new Tenant().setTenantCode("T-VALID").setTenantName("有效租户"));

        assertThat(tenantService.validateTenant(tenant.getTenantId())).isTrue();
        assertThat(tenantService.isTenantAccessible(tenant.getTenantId())).isTrue();
    }

    @Test
    @DisplayName("validateTenant - 校验已停用租户")
    void testValidateTenantInactive() {
        Tenant tenant = tenantService.createTenant(
                new Tenant().setTenantCode("T-INACT").setTenantName("停用租户"));
        tenantService.deactivateTenant(tenant.getTenantId());

        assertThat(tenantService.validateTenant(tenant.getTenantId())).isFalse();
    }

    @Test
    @DisplayName("validateTenant - 校验不存在租户")
    void testValidateTenantNotFound() {
        assertThat(tenantService.validateTenant("non-existent-id")).isFalse();
    }

    @Test
    @DisplayName("TenantValidator - ValidationResult")
    void testTenantValidatorResult() {
        Tenant tenant = tenantService.createTenant(
                new Tenant().setTenantCode("T-VR").setTenantName("校验结果租户"));

        TenantValidator.ValidationResult valid = tenantValidator.validate(tenant.getTenantId());
        assertThat(valid.valid()).isTrue();
        assertThat(valid.tenant()).isNotNull();
        assertThat(valid.tenant().getTenantId()).isEqualTo(tenant.getTenantId());

        TenantValidator.ValidationResult invalid = tenantValidator.validate("no-such-tenant");
        assertThat(invalid.valid()).isFalse();
        assertThat(invalid.message()).contains("租户不存在");
    }

    // ========== TenantContextHolder ==========

    @Test
    @DisplayName("TenantContextHolder - 基础读写")
    void testTenantContextHolder() {
        TenantContextHolder.setTenantId("ctx-001");
        assertThat(TenantContextHolder.getTenantId()).isEqualTo("ctx-001");

        TenantContextHolder.setTenantCode("CODE-001");
        assertThat(TenantContextHolder.getTenantCode()).isEqualTo("CODE-001");

        TenantContextHolder.setTenantName("Context租户");
        assertThat(TenantContextHolder.getTenantName()).isEqualTo("Context租户");

        TenantContextHolder.clear();
        assertThat(TenantContextHolder.getTenantId()).isNull();
        assertThat(TenantContextHolder.getTenantCode()).isNull();
    }

    @Test
    @DisplayName("TenantContextHolder - 完整上下文设置")
    void testTenantContextHolderFull() {
        Tenant tenant = new Tenant();
        tenant.setTenantId("ctx-full");
        tenant.setTenantCode("CTX-FULL");
        tenant.setTenantName("完整上下文租户");
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setTenantType(TenantType.ENTERPRISE);

        TenantContextHolder.setTenantContext(tenant);

        assertThat(TenantContextHolder.getTenantId()).isEqualTo("ctx-full");
        assertThat(TenantContextHolder.getTenantCode()).isEqualTo("CTX-FULL");
        assertThat(TenantContextHolder.getTenantName()).isEqualTo("完整上下文租户");
        assertThat(TenantContextHolder.getTenantStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(TenantContextHolder.getTenantType()).isEqualTo(TenantType.ENTERPRISE);
    }

    @Test
    @DisplayName("TenantContextHolder - executeWithTenant 嵌套切换")
    void testExecuteWithTenant() {
        TenantContextHolder.setTenantId("tenant-A");
        assertThat(TenantContextHolder.getTenantId()).isEqualTo("tenant-A");

        TenantContextHolder.executeWithTenant("tenant-B", () -> {
            assertThat(TenantContextHolder.getTenantId()).isEqualTo("tenant-B");

            TenantContextHolder.executeWithTenant("tenant-C", () -> {
                assertThat(TenantContextHolder.getTenantId()).isEqualTo("tenant-C");
            });

            assertThat(TenantContextHolder.getTenantId()).isEqualTo("tenant-B");
        });

        assertThat(TenantContextHolder.getTenantId()).isEqualTo("tenant-A");
    }

    @Test
    @DisplayName("TenantContextHolder - ignore 租户隔离")
    void testIgnoreTenantIsolation() {
        assertThat(TenantContextHolder.isIgnore()).isFalse();

        TenantContextHolder.executeWithoutTenant(() -> {
            assertThat(TenantContextHolder.isIgnore()).isTrue();
        });

        assertThat(TenantContextHolder.isIgnore()).isFalse();
    }

    // ========== @TenantSwitch 声明式切换 ==========

    @Test
    @DisplayName("@TenantSwitch - 字面值租户切换")
    void testTenantSwitchWithLiteral() {
        TenantContextHolder.setTenantId("original");

        String result = tenantSwitchTestService.withLiteralTenant("T-LITERAL");
        assertThat(result).isEqualTo("T-LITERAL");

        assertThat(TenantContextHolder.getTenantId()).isEqualTo("original");
    }

    @Test
    @DisplayName("@TenantSwitch - SpEL表达式租户切换")
    void testTenantSwitchWithSpel() {
        TenantContextHolder.setTenantId("original");

        String result = tenantSwitchTestService.withSpelTenant("T-SPEL");
        assertThat(result).isEqualTo("T-SPEL");

        assertThat(TenantContextHolder.getTenantId()).isEqualTo("original");
    }

    @Test
    @DisplayName("@TenantSwitch - 不恢复模式（永久切换）")
    void testTenantSwitchNoRestore() {
        TenantContextHolder.setTenantId("before");

        tenantSwitchTestService.noRestoreTenant("T-NORESTORE");

        // restoreAfter=false 时，方法执行后上下文被清除
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    @DisplayName("TenantService 注入验证")
    void testTenantServiceInjection() {
        assertThat(tenantService).isNotNull();
        assertThat(tenantValidator).isNotNull();
    }

    @Test
    @DisplayName("Tenant.isValid - 租户模型有效性判断")
    void testTenantModelIsValid() {
        Tenant activeTenant = new Tenant();
        activeTenant.setTenantId("m-valid");
        activeTenant.setStatus(TenantStatus.ACTIVE);
        assertThat(activeTenant.isValid()).isTrue();

        Tenant inactiveTenant = new Tenant();
        inactiveTenant.setTenantId("m-invalid");
        inactiveTenant.setStatus(TenantStatus.INACTIVE);
        assertThat(inactiveTenant.isValid()).isFalse();
    }

    @Test
    @DisplayName("Tenant 模型 - 配额使用率计算")
    void testTenantQuotaUsageRate() {
        Tenant tenant = new Tenant();
        tenant.setTenantId("m-quota");
        tenant.setQuota(100L);
        tenant.setUsedQuota(25L);
        assertThat(tenant.getQuotaUsageRate()).isEqualTo(0.25);
    }

    /**
     * 用于测试 @TenantSwitch 注解的内部服务类
     */
    @Service
    static class TenantSwitchTestService {

        @TenantSwitch(tenantId = "T-LITERAL", restoreAfter = true)
        public String withLiteralTenant(String input) {
            return TenantContextHolder.getTenantId();
        }

        @TenantSwitch(tenantId = "#tenantId", restoreAfter = true)
        public String withSpelTenant(String tenantId) {
            return TenantContextHolder.getTenantId();
        }

        @TenantSwitch(tenantId = "#tenantId", restoreAfter = false)
        public void noRestoreTenant(String tenantId) {
            // 仅验证上下文切换
        }
    }
}
