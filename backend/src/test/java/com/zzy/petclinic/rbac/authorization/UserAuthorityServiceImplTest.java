package com.zzy.petclinic.rbac.authorization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.zzy.petclinic.rbac.system.dataObject.SysPermission;
import com.zzy.petclinic.rbac.system.mapper.SysPermissionMapper;
import com.zzy.petclinic.rbac.system.dataObject.SysRole;
import com.zzy.petclinic.rbac.system.mapper.SysRoleMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserAuthorityServiceImplTest {
  private SysRoleMapper roleMapper;
  private SysPermissionMapper permissionMapper;
  private UserAuthorityServiceImpl service;

  @BeforeEach
  void setUp() {
    roleMapper = mock(SysRoleMapper.class);
    permissionMapper = mock(SysPermissionMapper.class);
    service = new UserAuthorityServiceImpl(roleMapper, permissionMapper);
  }

  @Test
  void loadsTheRequestedUsersRolesAndPermissionsWithoutAuthenticationContext() {
    when(roleMapper.selectByUserId(7L))
        .thenReturn(List.of(role("ADMIN"), role("ADMIN"), role(" ")));
    when(permissionMapper.selectByUserId(7L))
        .thenReturn(List.of(permission("system:manage"), permission("system:manage")));

    UserAuthorities result = service.loadByUserId(7L);

    assertEquals(Set.of("ADMIN"), result.roleCodes());
    assertEquals(Set.of("system:manage"), result.permissionCodes());
    verify(roleMapper).selectByUserId(7L);
    verify(permissionMapper).selectByUserId(7L);
  }

  @Test
  void keepsRoleAuthoritiesWhenNoFineGrainedPermissionIsAssigned() {
    when(roleMapper.selectByUserId(8L)).thenReturn(List.of(role("AUDITOR")));
    when(permissionMapper.selectByUserId(8L)).thenReturn(List.of());

    UserAuthorities result = service.loadByUserId(8L);

    assertEquals(Set.of("AUDITOR"), result.roleCodes());
    assertEquals(Set.of(), result.permissionCodes());
    assertEquals(List.of("ROLE_AUDITOR"), result.authorityCodes());
  }

  @Test
  void nullUserIdReturnsEmptyWithoutQueryingDatabase() {
    assertEquals(UserAuthorities.empty(), service.loadByUserId(null));
    verifyNoInteractions(roleMapper, permissionMapper);
  }

  @Test
  void userWithoutRolesReturnsEmptyAndSkipsPermissionQuery() {
    when(roleMapper.selectByUserId(9L)).thenReturn(List.of());

    assertEquals(UserAuthorities.empty(), service.loadByUserId(9L));
    verify(permissionMapper, never()).selectByUserId(9L);
  }

  private static SysRole role(String code) {
    SysRole role = new SysRole();
    role.setCode(code);
    return role;
  }

  private static SysPermission permission(String code) {
    SysPermission permission = new SysPermission();
    permission.setCode(code);
    return permission;
  }
}
