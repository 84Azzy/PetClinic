package com.zzy.petclinic.rbac.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zzy.petclinic.rbac.system.controller.PermissionController;
import com.zzy.petclinic.rbac.system.dataObject.SysPermission;
import com.zzy.petclinic.rbac.system.dataObject.SystemRequests;
import com.zzy.petclinic.rbac.system.systemServices.SystemServices;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class PermissionControllerTest {
  private SystemServices.PermissionService service;
  private PermissionController controller;

  @BeforeEach
  void setUp() {
    service = mock(SystemServices.PermissionService.class);
    controller = new PermissionController(service);
  }

  @Test
  void readEndpointsDelegateToService() {
    List<SysPermission> completeTree = List.of(permission(1L, "system:manage"));
    List<SysPermission> mineTree = List.of(permission(2L, "pet:manage"));
    when(service.tree()).thenReturn(completeTree);
    when(service.mine()).thenReturn(mineTree);

    assertSame(completeTree, controller.tree().data());
    assertSame(mineTree, controller.mine().data());
    verify(service).tree();
    verify(service).mine();
  }

  @Test
  void writeEndpointsDelegateAndKeepResponseSemantics() {
    SystemRequests.PermissionSave request =
        new SystemRequests.PermissionSave(
            null, "audit:read", "审计查看", "MENU", "/audit", "List", 10, null);
    SysPermission created = permission(9L, "audit:read");
    SysPermission updated = permission(9L, "audit:list");
    when(service.create(request)).thenReturn(created);
    when(service.update(9L, request)).thenReturn(updated);

    assertSame(created, controller.create(request).data());
    assertEquals(201, controller.create(request).code());
    assertSame(updated, controller.update(9L, request).data());
    assertEquals("删除权限节点成功", controller.delete(9L).message());
    verify(service).delete(9L);
  }

  @Test
  void managementRequiresSystemPermissionWhileMineOnlyRequiresLogin() throws Exception {
    assertEquals(
        "hasAuthority('system:manage')",
        PermissionController.class.getAnnotation(PreAuthorize.class).value());

    Method mine = PermissionController.class.getMethod("mine");
    assertEquals("isAuthenticated()", mine.getAnnotation(PreAuthorize.class).value());
  }

  private static SysPermission permission(Long id, String code) {
    SysPermission permission = new SysPermission();
    permission.setId(id);
    permission.setCode(code);
    return permission;
  }
}
