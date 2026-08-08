package cn.aryee.examples.tenant.reactive;

import cn.aryee.tenant.api.enums.TenantStatus;
import cn.aryee.tenant.api.enums.TenantType;
import cn.aryee.tenant.api.model.Tenant;
import cn.aryee.tenant.infrastructure.reactive.service.ReactiveTenantService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Aryee Tenant Reactive 模式集成测试
 * 验证 DefaultReactiveTenantService 的 CRUD、生命周期、配额管理等功能
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootTest
@DisplayName("Tenant 多租户集成测试 - Reactive 模式")
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class TenantReactiveIntegrationTest {

    private final ReactiveTenantService reactiveTenantService;

    // ========== CRUD ==========

    @Test
    @DisplayName("createTenant + getTenantById - 创建与获取租户")
    void testCreateAndGetTenant() {
        Tenant tenant = new Tenant()
                .setTenantCode("RT001")
                .setTenantName("响应式租户001")
                .setTenantType(TenantType.ENTERPRISE)
                .setQuota(1000L);

        reactiveTenantService.createTenant(tenant)
                .as(StepVerifier::create)
                .assertNext(created -> {
                    assertThat(created).isNotNull();
                    assertThat(created.getTenantId()).isNotNull().isNotEmpty();
                    assertThat(created.getStatus()).isEqualTo(TenantStatus.ACTIVE);
                    assertThat(created.getUsedQuota()).isEqualTo(0L);
                    assertThat(created.getCreateTime()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getTenantByCode - 根据编码获取租户")
    void testGetTenantByCode() {
        Tenant tenant = new Tenant()
                .setTenantCode("RT-CODE")
                .setTenantName("编码查询响应式租户");

        reactiveTenantService.createTenant(tenant).block();

        reactiveTenantService.getTenantByCode("RT-CODE")
                .as(StepVerifier::create)
                .assertNext(fetched -> {
                    assertThat(fetched.getTenantName()).isEqualTo("编码查询响应式租户");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getTenantByCode - 不存在返回 Mono.empty()")
    void testGetTenantByCodeNotFound() {
        reactiveTenantService.getTenantByCode("NONEXISTENT")
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    @DisplayName("updateTenant - 更新租户信息")
    void testUpdateTenant() {
        Tenant tenant = new Tenant()
                .setTenantCode("RT-UPD")
                .setTenantName("原始名称");
        Tenant created = reactiveTenantService.createTenant(tenant).block();

        created.setTenantName("更新后名称");
        reactiveTenantService.updateTenant(created)
                .as(StepVerifier::create)
                .assertNext(updated -> {
                    assertThat(updated.getTenantName()).isEqualTo("更新后名称");
                    assertThat(updated.getCreateTime()).isNotNull();
                    assertThat(updated.getUpdateTime()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("deleteTenant - 删除租户")
    void testDeleteTenant() {
        Tenant tenant = new Tenant()
                .setTenantCode("RT-DEL")
                .setTenantName("待删除租户");
        Tenant created = reactiveTenantService.createTenant(tenant).block();

        reactiveTenantService.deleteTenant(created.getTenantId())
                .as(StepVerifier::create)
                .verifyComplete();

        reactiveTenantService.getTenantById(created.getTenantId())
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    @DisplayName("listAllTenants - 获取所有租户")
    void testListAllTenants() {
        reactiveTenantService.createTenant(
                new Tenant().setTenantCode("RT-L1").setTenantName("列表1")).block();
        reactiveTenantService.createTenant(
                new Tenant().setTenantCode("RT-L2").setTenantName("列表2")).block();
        reactiveTenantService.createTenant(
                new Tenant().setTenantCode("RT-L3").setTenantName("列表3")).block();

        reactiveTenantService.listAllTenants()
                .collectList()
                .as(StepVerifier::create)
                .assertNext(tenants -> {
                    assertThat(tenants).isNotEmpty();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("listTenantsByStatus - 按状态查询租户")
    void testListTenantsByStatus() {
        reactiveTenantService.createTenant(
                new Tenant().setTenantCode("RT-S1").setTenantName("活跃租户1")).block();

        Tenant inactive = reactiveTenantService.createTenant(
                new Tenant().setTenantCode("RT-S2").setTenantName("停用租户")).block();
        reactiveTenantService.deactivateTenant(inactive.getTenantId()).block();

        reactiveTenantService.listTenantsByStatus(TenantStatus.ACTIVE)
                .collectList()
                .as(StepVerifier::create)
                .assertNext(tenants -> {
                    assertThat(tenants).anyMatch(t -> "RT-S1".equals(t.getTenantCode()));
                })
                .verifyComplete();

        reactiveTenantService.listTenantsByStatus(TenantStatus.INACTIVE)
                .collectList()
                .as(StepVerifier::create)
                .assertNext(tenants -> {
                    assertThat(tenants).anyMatch(t -> "RT-S2".equals(t.getTenantCode()));
                })
                .verifyComplete();
    }

    // ========== 生命周期 ==========

    @Test
    @DisplayName("activateTenant - 激活租户")
    void testActivateTenant() {
        Tenant tenant = reactiveTenantService.createTenant(
                new Tenant().setTenantCode("RT-ACT").setTenantName("待激活租户")).block();
        reactiveTenantService.deactivateTenant(tenant.getTenantId()).block();

        reactiveTenantService.activateTenant(tenant.getTenantId())
                .as(StepVerifier::create)
                .assertNext(activated -> {
                    assertThat(activated.getStatus()).isEqualTo(TenantStatus.ACTIVE);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("deactivateTenant - 停用租户")
    void testDeactivateTenant() {
        Tenant tenant = reactiveTenantService.createTenant(
                new Tenant().setTenantCode("RT-DEACT").setTenantName("待停用租户")).block();

        reactiveTenantService.deactivateTenant(tenant.getTenantId())
                .as(StepVerifier::create)
                .assertNext(deactivated -> {
                    assertThat(deactivated.getStatus()).isEqualTo(TenantStatus.INACTIVE);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("suspendTenant - 挂起租户")
    void testSuspendTenant() {
        Tenant tenant = reactiveTenantService.createTenant(
                new Tenant().setTenantCode("RT-SUS").setTenantName("待挂起租户")).block();

        reactiveTenantService.suspendTenant(tenant.getTenantId())
                .as(StepVerifier::create)
                .assertNext(suspended -> {
                    assertThat(suspended.getStatus()).isEqualTo(TenantStatus.SUSPENDED);
                })
                .verifyComplete();
    }

    // ========== 校验 ==========

    @Test
    @DisplayName("validateTenant - 校验有效租户")
    void testValidateTenantValid() {
        Tenant tenant = reactiveTenantService.createTenant(
                new Tenant().setTenantCode("RT-VALID").setTenantName("有效租户")).block();

        reactiveTenantService.validateTenant(tenant.getTenantId())
                .as(StepVerifier::create)
                .assertNext(valid -> {
                    assertThat(valid).isTrue();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("validateTenant - 校验已停用租户")
    void testValidateTenantInactive() {
        Tenant tenant = reactiveTenantService.createTenant(
                new Tenant().setTenantCode("RT-INACT").setTenantName("停用租户")).block();
        reactiveTenantService.deactivateTenant(tenant.getTenantId()).block();

        reactiveTenantService.validateTenant(tenant.getTenantId())
                .as(StepVerifier::create)
                .assertNext(valid -> {
                    assertThat(valid).isFalse();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("validateTenant - 校验不存在租户")
    void testValidateTenantNotFound() {
        reactiveTenantService.validateTenant("non-existent-id")
                .as(StepVerifier::create)
                .assertNext(valid -> {
                    assertThat(valid).isFalse();
                })
                .verifyComplete();
    }

    // ========== 配额 ==========

    @Test
    @DisplayName("checkQuota - 检查配额")
    void testCheckQuota() {
        Tenant tenant = reactiveTenantService.createTenant(
                new Tenant().setTenantCode("RT-QUOTA")
                        .setTenantName("配额租户")
                        .setQuota(100L)
                        .setUsedQuota(30L)).block();

        reactiveTenantService.checkQuota(tenant.getTenantId(), 50L)
                .as(StepVerifier::create)
                .assertNext(result -> assertThat(result).isTrue())
                .verifyComplete();

        reactiveTenantService.checkQuota(tenant.getTenantId(), 80L)
                .as(StepVerifier::create)
                .assertNext(result -> assertThat(result).isFalse())
                .verifyComplete();
    }

    // ========== 注入验证 ==========

    @Test
    @DisplayName("ReactiveTenantService 注入验证")
    void testReactiveTenantServiceInjection() {
        assertThat(reactiveTenantService).isNotNull();
    }

    // ========== 错误场景 ==========

    @Test
    @DisplayName("createTenant - null 参数返回错误")
    void testCreateTenantNull() {
        reactiveTenantService.createTenant(null)
                .as(StepVerifier::create)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException
                                && throwable.getMessage().contains("must not be null"))
                .verify();
    }

    @Test
    @DisplayName("getTenantById - null/empty 返回 Mono.empty()")
    void testGetTenantByIdNull() {
        reactiveTenantService.getTenantById(null)
                .as(StepVerifier::create)
                .verifyComplete();

        reactiveTenantService.getTenantById("")
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    @DisplayName("updateTenant - 不存在的租户返回错误")
    void testUpdateTenantNotFound() {
        Tenant nonExistent = new Tenant();
        nonExistent.setTenantId("non-existent-id");
        nonExistent.setTenantCode("RT-NF");
        nonExistent.setTenantName("不存在租户");

        reactiveTenantService.updateTenant(nonExistent)
                .as(StepVerifier::create)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException)
                .verify();
    }

    // ========== Tenant 模型测试 ==========

    @Test
    @DisplayName("Tenant.isValid - 模型有效性判断")
    void testTenantModelIsValid() {
        Tenant activeTenant = new Tenant();
        activeTenant.setTenantId("rm-valid");
        activeTenant.setStatus(TenantStatus.ACTIVE);
        assertThat(activeTenant.isValid()).isTrue();

        Tenant inactiveTenant = new Tenant();
        inactiveTenant.setTenantId("rm-invalid");
        inactiveTenant.setStatus(TenantStatus.INACTIVE);
        assertThat(inactiveTenant.isValid()).isFalse();
    }

    @Test
    @DisplayName("Tenant.hasEnoughQuota - 配额检查")
    void testTenantModelHasEnoughQuota() {
        Tenant tenant = new Tenant();
        tenant.setTenantId("rm-q");
        tenant.setQuota(100L);
        tenant.setUsedQuota(30L);

        assertThat(tenant.hasEnoughQuota(50L)).isTrue();
        assertThat(tenant.hasEnoughQuota(80L)).isFalse();
        assertThat(tenant.hasEnoughQuota(null)).isFalse();

        // 无限配额
        Tenant unlimited = new Tenant();
        unlimited.setTenantId("rm-qu");
        unlimited.setQuota(-1L);
        unlimited.setUsedQuota(999999L);
        assertThat(unlimited.hasEnoughQuota(9999999L)).isTrue();
    }

    @Test
    @DisplayName("Tenant 生命周期方法")
    void testTenantLifecycleMethods() {
        Tenant tenant = new Tenant();
        tenant.setTenantId("rm-life");
        tenant.setStatus(TenantStatus.ACTIVE);

        tenant.deactivate();
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.INACTIVE);

        tenant.activate();
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);

        tenant.suspend();
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.SUSPENDED);

        tenant.cancel();
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.CANCELLED);
    }
}
