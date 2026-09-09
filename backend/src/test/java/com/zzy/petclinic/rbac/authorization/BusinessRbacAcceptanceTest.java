package com.zzy.petclinic.rbac.authorization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zzy.petclinic.common.PageQuery;
import com.zzy.petclinic.audit.OperationLogController;
import com.zzy.petclinic.audit.OperationLogService;
import com.zzy.petclinic.dashboard.DashboardController;
import com.zzy.petclinic.dashboard.DashboardService;
import com.zzy.petclinic.medical.MedicalRecordController;
import com.zzy.petclinic.medical.MedicalRecordService;
import com.zzy.petclinic.notice.NoticeController;
import com.zzy.petclinic.notice.NoticeRequest;
import com.zzy.petclinic.notice.NoticeService;
import com.zzy.petclinic.owner.Owner;
import com.zzy.petclinic.owner.OwnerService;
import com.zzy.petclinic.pet.Pet;
import com.zzy.petclinic.pet.PetController;
import com.zzy.petclinic.pet.PetMapper;
import com.zzy.petclinic.pet.PetRequest;
import com.zzy.petclinic.pet.PetService;
import com.zzy.petclinic.pet.PetServiceImpl;
import com.zzy.petclinic.rbac.authentication.AuthenticatedUser;
import com.zzy.petclinic.rbac.authentication.CurrentUser;
import com.zzy.petclinic.rbac.authentication.SysUser;
import com.zzy.petclinic.rbac.system.controller.PermissionController;
import com.zzy.petclinic.rbac.system.SystemServices.SystemServices;
import com.zzy.petclinic.schedule.ScheduleController;
import com.zzy.petclinic.schedule.ScheduleService;
import com.zzy.petclinic.schedule.SlotRequest;
import com.zzy.petclinic.vaccination.VaccinationController;
import com.zzy.petclinic.vaccination.VaccinationService;
import com.zzy.petclinic.visit.CancelVisitRequest;
import com.zzy.petclinic.visit.VisitController;
import com.zzy.petclinic.visit.VisitRequest;
import com.zzy.petclinic.visit.VisitServiceImpl;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** 验证业务入口与 Service 两层 RBAC，并锁定混合读写接口的权限矩阵。 */
@SpringBootTest
class BusinessRbacAcceptanceTest {
  @Autowired private PetController petController;
  @Autowired private PetService petService;

  @MockBean private PetMapper petMapper;
  @MockBean private CurrentUser currentUser;
  @MockBean private OwnerService ownerService;

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void ownerCannotUpdatePetEvenWhenFineGrainedPetPermissionsArePresent() {
    authenticate(
        3L,
        Set.of("OWNER"),
        Set.of("pet:manage", "pet:create", "pet:update", "pet:delete"));

    assertThrows(AccessDeniedException.class, () -> petController.update(8L, null));
    assertThrows(AccessDeniedException.class, () -> petService.update(8L, null));
    verify(petMapper, never()).selectById(8L);
  }

  @Test
  void staffCanReadPetThroughControllerAndServiceLayers() {
    AuthenticatedUser staff = authenticate(2L, Set.of("STAFF"), Set.of("pet:manage"));
    when(currentUser.require()).thenReturn(staff);
    Pet expected = new Pet();
    expected.setId(8L);
    when(petMapper.selectById(8L)).thenReturn(expected);

    assertSame(expected, petController.get(8L).data());
    assertSame(expected, petService.get(8L));
  }

  @Test
  void staffCanCreatePetWhenRoleAndActionPermissionAreBothPresent() {
    authenticate(2L, Set.of("STAFF"), Set.of("pet:create"));
    PetRequest request =
        new PetRequest(
            1L,
            1L,
            "糯米",
            "FEMALE",
            "英短",
            LocalDate.of(2022, 4, 12),
            null,
            null,
            null,
            null);

    assertEquals(201, petController.create(request).code());
    verify(petMapper).insert(org.mockito.ArgumentMatchers.any(Pet.class));
  }

  @Test
  void ownerCannotQueryAnotherOwnersPetsThroughService() {
    AuthenticatedUser owner = authenticate(3L, Set.of("OWNER"), Set.of("pet:manage"));
    when(currentUser.require()).thenReturn(owner);
    Owner profile = new Owner();
    profile.setId(10L);
    when(ownerService.mine()).thenReturn(profile);

    assertThrows(
        AccessDeniedException.class,
        () -> petService.page(new PageQuery(1L, 10L, null, null), 11L));
    verify(petMapper, never())
        .selectPage(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void anonymousCallerCannotBypassServiceByCallingItDirectly() {
    assertThrows(AuthenticationCredentialsNotFoundException.class, () -> petService.mine());
  }

  @Test
  void mixedReadWritePermissionMatrixIsDeclaredOnBothLayers() throws Exception {
    assertMethodRule(
        PetController.class,
        "create",
        "hasAuthority('pet:create') && hasAnyRole('ADMIN', 'STAFF')",
        PetRequest.class);
    assertMethodRule(
        PetServiceImpl.class,
        "create",
        "hasAuthority('pet:create') && hasAnyRole('ADMIN', 'STAFF')",
        PetRequest.class);
    assertMethodRule(
        VisitController.class,
        "cancel",
        "hasAuthority('visit:cancel')",
        Long.class,
        CancelVisitRequest.class);
    assertMethodRule(
        VisitServiceImpl.class,
        "create",
        "hasAuthority('visit:create')",
        VisitRequest.class);
    assertMethodRule(
        ScheduleController.class,
        "create",
        "hasAuthority('schedule:manage') && hasAnyRole('ADMIN', 'STAFF')",
        SlotRequest.class);
    assertMethodRule(ScheduleService.class, "available", "isAuthenticated()", Long.class, java.time.LocalDate.class);
    assertMethodRule(NoticeController.class, "active", "isAuthenticated()");
    assertMethodRule(
        NoticeService.class,
        "create",
        "hasAuthority('notice:manage') && hasAnyRole('ADMIN', 'STAFF')",
        NoticeRequest.class);
  }

  @Test
  void sensitiveModulesAndSystemManagementHaveClassLevelRules() {
    assertEquals("hasAuthority('dashboard:read')", ruleOn(DashboardController.class));
    assertEquals("hasAuthority('dashboard:read')", ruleOn(DashboardService.class));
    assertEquals("hasAuthority('system:manage')", ruleOn(OperationLogController.class));
    assertEquals("hasAuthority('system:manage')", ruleOn(OperationLogService.class));

    String clinicalRule =
        "hasAuthority('medical:manage') && hasAnyRole('ADMIN', 'STAFF')";
    assertEquals(clinicalRule, ruleOn(MedicalRecordController.class));
    assertEquals(clinicalRule, ruleOn(MedicalRecordService.class));

    String vaccinationRule =
        "hasAuthority('vaccination:manage') && hasAnyRole('ADMIN', 'STAFF')";
    assertEquals(vaccinationRule, ruleOn(VaccinationController.class));
    assertEquals(vaccinationRule, ruleOn(VaccinationService.class));

    assertEquals("hasAuthority('system:manage')", ruleOn(PermissionController.class));
    assertEquals("hasAuthority('system:manage')", ruleOn(SystemServices.UserAdminService.class));
  }

  private AuthenticatedUser authenticate(
      Long id, Set<String> roles, Set<String> permissions) {
    SysUser entity = new SysUser();
    entity.setId(id);
    entity.setUsername("user-" + id);
    entity.setPasswordHash("password");
    entity.setStatus("ACTIVE");
    AuthenticatedUser principal =
        new AuthenticatedUser(entity, new UserAuthorities(roles, permissions));
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()));
    return principal;
  }

  private static void assertMethodRule(
      Class<?> type, String name, String expected, Class<?>... parameterTypes) throws Exception {
    Method method = type.getMethod(name, parameterTypes);
    assertEquals(expected, method.getAnnotation(PreAuthorize.class).value());
  }

  private static String ruleOn(Class<?> type) {
    return type.getAnnotation(PreAuthorize.class).value();
  }
}
