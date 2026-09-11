package com.zzy.petclinic.rbac.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.zzy.petclinic.rbac.system.systemServices.SystemServices;
import com.zzy.petclinic.rbac.system.controller.RoleController;
import com.zzy.petclinic.rbac.system.dataObject.SysRole;
import com.zzy.petclinic.rbac.system.dataObject.SystemRequests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class RoleControllerTest {
  private SystemServices.RoleService service;
  private RoleController controller;

  @BeforeEach
  void setUp() {
    service = mock(SystemServices.RoleService.class);
    controller = new RoleController(service);
  }

  @Test
  void readAndCreateEndpointsDelegateToService() {
    List<SysRole> roles = List.of(new SysRole());
    SystemRequests.RoleSave request =
        new SystemRequests.RoleSave("VET", "兽医", null, null);
    SysRole created = new SysRole();
    when(service.list()).thenReturn(roles);
    when(service.create(request)).thenReturn(created);

    assertSame(roles, controller.list().data());
    assertSame(created, controller.create(request).data());
    assertEquals(201, controller.create(request).code());
  }

  @Test
  void deleteAndPermissionEndpointsPassRoleIdRatherThanUserId() {
    SystemRequests.Ids request = new SystemRequests.Ids(List.of(10L, 11L));

    assertEquals("删除角色成功", controller.delete(7L).message());
    assertEquals("角色权限替换成功", controller.permissions(7L, request).message());
    verify(service).delete(7L);
    verify(service).assignPermissions(7L, List.of(10L, 11L));
  }

  @Test
  void controllerRequiresSystemManagePermission() {
    assertEquals(
        "hasAuthority('system:manage')",
        RoleController.class.getAnnotation(PreAuthorize.class).value());
  }
}
