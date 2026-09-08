package com.zzy.petclinic.rbac.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.zzy.petclinic.common.BusinessException;
import com.zzy.petclinic.rbac.system.SystemServicesImpl.RoleServiceImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;

class RoleServiceImplTest {
  private SysRoleMapper roleMapper;
  private SysPermissionMapper permissionMapper;
  private RoleServiceImpl service;

  @BeforeEach
  void setUp() {
    roleMapper = mock(SysRoleMapper.class);
    permissionMapper = mock(SysPermissionMapper.class);
    service = new RoleServiceImpl(roleMapper, permissionMapper);
  }

  @Test
  void createChecksUniquenessDefaultsStatusAndWritesAuditTimeBeforeInsert() {
    SystemRequests.RoleSave request =
        new SystemRequests.RoleSave(" VET ", " 兽医 ", "接诊角色", null);
    when(roleMapper.selectOne(any(Wrapper.class))).thenReturn(null);
    when(roleMapper.insert(any(SysRole.class)))
        .thenAnswer(
            invocation -> {
              SysRole role = invocation.getArgument(0);
              assertNotNull(role.getCreatedAt());
              assertNotNull(role.getUpdatedAt());
              return 1;
            });

    SysRole created = service.create(request);

    assertEquals("VET", created.getCode());
    assertEquals("兽医", created.getName());
    assertEquals("ACTIVE", created.getStatus());
    verify(roleMapper).selectOne(any(Wrapper.class));
  }

  @Test
  void createRejectsReservedOrDuplicateCode() {
    BusinessException reserved =
        assertThrows(
            BusinessException.class,
            () -> service.create(new SystemRequests.RoleSave("admin", "管理员", null, null)));
    assertEquals(HttpStatus.BAD_REQUEST, reserved.getStatus());
    verifyNoInteractions(roleMapper);

    SysRole duplicate = role(9L, "VET", "ACTIVE");
    when(roleMapper.selectOne(any(Wrapper.class))).thenReturn(duplicate);
    BusinessException conflict =
        assertThrows(
            BusinessException.class,
            () -> service.create(new SystemRequests.RoleSave("VET", "兽医", null, null)));
    assertEquals(HttpStatus.CONFLICT, conflict.getStatus());
    verify(roleMapper, never()).insert(any(SysRole.class));
  }

  @Test
  void updateChangesNameAndCanClearDescription() {
    SysRole role = role(7L, "VET", "ACTIVE");
    role.setName("旧名称");
    role.setDescription("旧描述");
    when(roleMapper.selectById(7L)).thenReturn(role);
    when(roleMapper.selectOne(any(Wrapper.class))).thenReturn(null);
    when(roleMapper.updateById(role)).thenReturn(1);
    SystemRequests.RoleSave request =
        new SystemRequests.RoleSave("DOCTOR", "新名称", null, "INACTIVE");

    SysRole updated = service.update(7L, request);

    assertEquals("DOCTOR", updated.getCode());
    assertEquals("新名称", updated.getName());
    assertNull(updated.getDescription());
    assertEquals("INACTIVE", updated.getStatus());
    assertNotNull(updated.getUpdatedAt());
    verify(roleMapper).updateById(role);
  }

  @Test
  void builtInRoleAllowsMetadataUpdateButNotCodeChange() {
    SysRole admin = role(1L, "ADMIN", "ACTIVE");
    admin.setName("管理员");
    when(roleMapper.selectById(1L)).thenReturn(admin);
    when(roleMapper.updateById(admin)).thenReturn(1);

    SysRole updated =
        service.update(1L, new SystemRequests.RoleSave("ADMIN", "超级管理员", "内置", null));
    assertEquals("超级管理员", updated.getName());

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                service.update(
                    1L, new SystemRequests.RoleSave("ROOT", "超级管理员", "内置", null)));
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
  }

  @Test
  void deleteRejectsRoleStillBoundToUsersAsConflict() {
    SysRole role = role(6L, "AUDITOR", "ACTIVE");
    when(roleMapper.selectById(6L)).thenReturn(role);
    when(roleMapper.countUsersByRoleId(6L)).thenReturn(2L);

    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.delete(6L));

    assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    verify(roleMapper, never()).deletePermissionsByRoleId(6L);
    verify(roleMapper, never()).deleteById(6L);
  }

  @Test
  void deleteClearsPermissionRelationsBeforeDeletingRole() {
    SysRole role = role(6L, "AUDITOR", "ACTIVE");
    when(roleMapper.selectById(6L)).thenReturn(role);
    when(roleMapper.countUsersByRoleId(6L)).thenReturn(0L);
    when(roleMapper.deleteById(6L)).thenReturn(1);

    service.delete(6L);

    var order = org.mockito.Mockito.inOrder(roleMapper);
    order.verify(roleMapper).deletePermissionsByRoleId(6L);
    order.verify(roleMapper).deleteById(6L);
  }

  @Test
  void assignPermissionsDeduplicatesIdsAndAllowsExistingInactivePermission() {
    when(roleMapper.selectById(5L)).thenReturn(role(5L, "VET", "ACTIVE"));
    SysPermission active = permission(10L, "ACTIVE");
    SysPermission inactive = permission(11L, "INACTIVE");
    when(permissionMapper.selectByIds(List.of(10L, 11L)))
        .thenReturn(List.of(active, inactive));
    when(roleMapper.insertRolePermissions(5L, List.of(10L, 11L))).thenReturn(2);

    service.assignPermissions(5L, List.of(10L, 11L, 10L));

    verify(roleMapper).deletePermissionsByRoleId(5L);
    verify(roleMapper).insertRolePermissions(5L, List.of(10L, 11L));
  }

  @Test
  void assignPermissionsRejectsMissingIdBeforeDeletingExistingRelations() {
    when(roleMapper.selectById(5L)).thenReturn(role(5L, "VET", "ACTIVE"));
    when(permissionMapper.selectByIds(List.of(10L, 99L)))
        .thenReturn(List.of(permission(10L, "ACTIVE")));

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> service.assignPermissions(5L, List.of(10L, 99L)));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    assertTrue(exception.getMessage().contains("99"));
    verify(roleMapper, never()).deletePermissionsByRoleId(5L);
  }

  @Test
  void implementationUsesTheSamePermissionCodeAsControllerContract() {
    assertEquals(
        "hasAuthority('system:manage')",
        RoleServiceImpl.class.getAnnotation(PreAuthorize.class).value());
  }

  private static SysRole role(Long id, String code, String status) {
    SysRole role = new SysRole();
    role.setId(id);
    role.setCode(code);
    role.setStatus(status);
    return role;
  }

  private static SysPermission permission(Long id, String status) {
    SysPermission permission = new SysPermission();
    permission.setId(id);
    permission.setCode("permission:" + id);
    permission.setStatus(status);
    return permission;
  }
}
