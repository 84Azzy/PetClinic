package com.zzy.petclinic.rbac.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.zzy.petclinic.common.BusinessException;
import com.zzy.petclinic.common.PageQuery;
import com.zzy.petclinic.common.PageResponse;
import com.zzy.petclinic.rbac.authentication.CurrentUser;
import com.zzy.petclinic.rbac.authentication.SysUser;
import com.zzy.petclinic.rbac.system.systemServices.impl.UserAdminServiceImpl;
import java.util.List;

import com.zzy.petclinic.rbac.system.dataObject.SysRole;
import com.zzy.petclinic.rbac.system.dataObject.SystemRequests;
import com.zzy.petclinic.rbac.system.mapper.SysRoleMapper;
import com.zzy.petclinic.rbac.system.mapper.SysUserAdminMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserAdminServiceImplTest {
  private SysUserAdminMapper userMapper;
  private PasswordEncoder passwordEncoder;
  private SysRoleMapper roleMapper;
  private CurrentUser currentUser;
  private UserAdminServiceImpl service;

  @BeforeEach
  void setUp() {
    userMapper = mock(SysUserAdminMapper.class);
    passwordEncoder = mock(PasswordEncoder.class);
    roleMapper = mock(SysRoleMapper.class);
    currentUser = mock(CurrentUser.class);
    service = new UserAdminServiceImpl(userMapper, passwordEncoder, roleMapper, currentUser);
  }

  @Test
  void pageNormalizesBlankFiltersAndUsesCorrectOffset() {
    SysUser user = user(3L, "zhangsan", "ACTIVE", 1);
    when(userMapper.selectPageUsers(null, null, 20, 10)).thenReturn(List.of(user));
    when(userMapper.countUsers(null, null)).thenReturn(21L);

    PageResponse<SysUser> page = service.page(new PageQuery(3L, 10L, "  ", ""));

    assertEquals(List.of(user), page.records());
    assertEquals(21L, page.total());
    assertEquals(3L, page.page());
    verify(userMapper).selectPageUsers(null, null, 20, 10);
    verify(userMapper).countUsers(null, null);
  }

  @Test
  void createHashesPasswordAndDoesNotReturnItsHash() {
    SystemRequests.UserSave request =
        new SystemRequests.UserSave("new-user", "新用户", "13800000000", null, "STAFF", "secret1");
    when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null);
    when(passwordEncoder.encode("secret1")).thenReturn("encoded-secret");
    when(userMapper.insert(any(SysUser.class))).thenReturn(1);

    SysUser created = service.create(request);

    assertEquals("new-user", created.getUsername());
    assertEquals("ACTIVE", created.getStatus());
    assertEquals(1, created.getTokenVersion());
    assertNull(created.getPasswordHash());
    verify(passwordEncoder).encode("secret1");
  }

  @Test
  void updateWritesOnlyTheLoadedEntityAndInvalidatesJwtWhenUsernameChanges() {
    SysUser original = user(7L, "old-name", "ACTIVE", 4);
    original.setPasswordHash("old-hash");
    when(userMapper.selectById(7L)).thenReturn(original);
    when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null);
    when(userMapper.updateById(original)).thenReturn(1);
    SystemRequests.UserSave request =
        new SystemRequests.UserSave("new-name", "新姓名", null, "new@example.com", "ADMIN", null);

    SysUser updated = service.update(7L, request);

    assertSame(original, updated);
    assertEquals("new-name", updated.getUsername());
    assertEquals("新姓名", updated.getDisplayName());
    assertEquals(5, updated.getTokenVersion());
    assertNull(updated.getPasswordHash());
    verify(userMapper).updateById(original);
  }

  @Test
  void changeStatusSetsRequestedValueAndInvalidatesOldToken() {
    SysUser user = user(8L, "staff", "ACTIVE", 2);
    when(userMapper.selectById(8L)).thenReturn(user);
    when(currentUser.id()).thenReturn(1L);
    when(userMapper.updateById(user)).thenReturn(1);

    service.changeStatus(8L, "INACTIVE");

    assertEquals("INACTIVE", user.getStatus());
    assertEquals(3, user.getTokenVersion());
    verify(userMapper).updateById(user);
  }

  @Test
  void changeStatusRejectsInvalidValueAndSelfDisable() {
    SysUser user = user(8L, "admin", "ACTIVE", 1);
    when(userMapper.selectById(8L)).thenReturn(user);

    BusinessException invalid =
        assertThrows(BusinessException.class, () -> service.changeStatus(8L, "DELETED"));
    assertEquals(HttpStatus.BAD_REQUEST, invalid.getStatus());

    when(currentUser.id()).thenReturn(8L);
    BusinessException selfDisable =
        assertThrows(BusinessException.class, () -> service.changeStatus(8L, "INACTIVE"));
    assertEquals(HttpStatus.CONFLICT, selfDisable.getStatus());
    verify(userMapper, never()).updateById(user);
  }

  @Test
  void resetPasswordExecutesUpdateAndInvalidatesOldToken() {
    SysUser user = user(5L, "owner", "ACTIVE", 6);
    when(userMapper.selectById(5L)).thenReturn(user);
    when(passwordEncoder.encode("newpass")).thenReturn("new-hash");
    when(userMapper.updateById(user)).thenReturn(1);

    service.resetPassword(5L, "newpass");

    assertEquals("new-hash", user.getPasswordHash());
    assertEquals(7, user.getTokenVersion());
    verify(userMapper).updateById(user);
  }

  @Test
  void assignRolesDeduplicatesBeforeReplacingRelations() {
    when(userMapper.selectById(9L)).thenReturn(user(9L, "staff", "ACTIVE", 1));
    SysRole admin = role(1L, "ACTIVE");
    SysRole auditor = role(2L, "ACTIVE");
    when(roleMapper.selectByIds(List.of(1L, 2L))).thenReturn(List.of(admin, auditor));
    when(userMapper.insertUserRoles(9L, List.of(1L, 2L))).thenReturn(2);

    service.assignRoles(9L, List.of(1L, 2L, 1L));

    verify(userMapper).deleteRolesByUserId(9L);
    verify(userMapper).insertUserRoles(9L, List.of(1L, 2L));
  }

  @Test
  void assignRolesRejectsMissingOrInactiveRolesBeforeDeletingOldRelations() {
    when(userMapper.selectById(9L)).thenReturn(user(9L, "staff", "ACTIVE", 1));
    when(roleMapper.selectByIds(List.of(1L, 2L))).thenReturn(List.of(role(1L, "INACTIVE")));

    assertThrows(BusinessException.class, () -> service.assignRoles(9L, List.of(1L, 2L)));

    verify(userMapper, never()).deleteRolesByUserId(9L);
    verify(userMapper, never()).insertUserRoles(any(), any());
  }

  private static SysUser user(Long id, String username, String status, Integer tokenVersion) {
    SysUser user = new SysUser();
    user.setId(id);
    user.setUsername(username);
    user.setDisplayName(username);
    user.setAccountType("STAFF");
    user.setStatus(status);
    user.setTokenVersion(tokenVersion);
    return user;
  }

  private static SysRole role(Long id, String status) {
    SysRole role = new SysRole();
    role.setId(id);
    role.setCode("ROLE_" + id);
    role.setStatus(status);
    return role;
  }
}
