package com.zzy.petclinic.rbac.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zzy.petclinic.common.PageQuery;
import com.zzy.petclinic.common.PageResponse;
import com.zzy.petclinic.rbac.authentication.SysUser;
import com.zzy.petclinic.rbac.system.SystemServices.SystemServices;
import com.zzy.petclinic.rbac.system.SystemServices.impl.UserAdminServiceImpl;
import java.util.List;

import com.zzy.petclinic.rbac.system.controller.UserAdminController;
import com.zzy.petclinic.rbac.system.dataObject.SystemRequests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class UserAdminControllerTest {
  private SystemServices.UserAdminService service;
  private UserAdminController controller;

  @BeforeEach
  void setUp() {
    service = mock(SystemServices.UserAdminService.class);
    controller = new UserAdminController(service);
  }

  @Test
  void pageAndCreateDelegateToServiceWithExpectedResponseCodes() {
    PageQuery query = new PageQuery(1L, 10L, null, null);
    PageResponse<SysUser> page = new PageResponse<>(List.of(), 0, 1, 10);
    SystemRequests.UserSave request =
        new SystemRequests.UserSave("staff", "员工", null, null, "STAFF", "secret1");
    SysUser created = new SysUser();
    when(service.page(query)).thenReturn(page);
    when(service.create(request)).thenReturn(created);

    assertSame(page, controller.page(query).data());
    assertEquals(200, controller.page(query).code());
    assertSame(created, controller.create(request).data());
    assertEquals(201, controller.create(request).code());
  }

  @Test
  void writeEndpointsInvokeServiceAndReturnSuccessMessages() {
    SystemRequests.StatusChange status = new SystemRequests.StatusChange("INACTIVE");
    SystemRequests.PasswordReset password = new SystemRequests.PasswordReset("secret1");
    SystemRequests.Ids roles = new SystemRequests.Ids(List.of(1L, 2L));

    assertEquals("用户状态修改成功", controller.status(7L, status).message());
    assertEquals("用户密码重置成功", controller.password(7L, password).message());
    assertEquals("用户角色分配成功", controller.roles(7L, roles).message());
    verify(service).changeStatus(7L, "INACTIVE");
    verify(service).resetPassword(7L, "secret1");
    verify(service).assignRoles(7L, List.of(1L, 2L));
  }

  @Test
  void controllerAndServiceImplementationDeclareSystemManagePermission() {
    assertEquals(
        "hasAuthority('system:manage')",
        UserAdminController.class.getAnnotation(PreAuthorize.class).value());
    assertEquals(
        "hasAuthority('system:manage')",
        UserAdminServiceImpl.class.getAnnotation(PreAuthorize.class).value());
  }
}
