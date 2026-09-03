package com.zzy.petclinic.rbac.authorization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.zzy.petclinic.rbac.system.SysPermission;
import com.zzy.petclinic.rbac.system.SysPermissionMapper;
import com.zzy.petclinic.rbac.system.SysRole;
import com.zzy.petclinic.rbac.system.SysRoleMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

/** 使用 H2 执行真实 RBAC SQL，验收状态过滤、去重、排序和实体映射。 */
@SpringBootTest
@Sql(
    statements = {
      "DROP TABLE IF EXISTS sys_role_permission",
      "DROP TABLE IF EXISTS sys_user_role",
      "DROP TABLE IF EXISTS sys_permission",
      "DROP TABLE IF EXISTS sys_role",
      "CREATE TABLE sys_role (id BIGINT PRIMARY KEY, code VARCHAR(50), name VARCHAR(50),"
          + " description VARCHAR(255), status VARCHAR(20), created_at TIMESTAMP, updated_at TIMESTAMP)",
      "CREATE TABLE sys_permission (id BIGINT PRIMARY KEY, parent_id BIGINT, code VARCHAR(100),"
          + " name VARCHAR(80), type VARCHAR(20), path VARCHAR(160), icon VARCHAR(60),"
          + " sort_order INT, status VARCHAR(20), created_at TIMESTAMP, updated_at TIMESTAMP)",
      "CREATE TABLE sys_user_role (user_id BIGINT, role_id BIGINT)",
      "CREATE TABLE sys_role_permission (role_id BIGINT, permission_id BIGINT)",
      "INSERT INTO sys_role VALUES"
          + " (1, 'ADMIN', '管理员', '全部权限', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),"
          + " (2, 'AUDITOR', '审计员', '只读', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),"
          + " (3, 'DISABLED', '停用角色', NULL, 'INACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
      "INSERT INTO sys_permission VALUES"
          + " (10, NULL, 'later:read', '稍后', 'BUTTON', NULL, NULL, 20, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),"
          + " (11, NULL, 'first:read', '优先', 'MENU', '/first', NULL, 10, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),"
          + " (12, NULL, 'disabled:read', '停用权限', 'MENU', NULL, NULL, 1, 'INACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),"
          + " (13, NULL, 'inactive-role:read', '停用角色权限', 'MENU', NULL, NULL, 1, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
      "INSERT INTO sys_user_role VALUES (7, 2), (7, 1), (7, 3)",
      "INSERT INTO sys_role_permission VALUES (1, 10), (2, 10), (1, 11), (1, 12), (3, 13)"
    })
class AuthorityMapperIntegrationTest {
  @Autowired private SysRoleMapper roleMapper;
  @Autowired private SysPermissionMapper permissionMapper;
  @Autowired private UserAuthorityService authorityService;

  @Test
  void queriesOnlyActiveAuthoritiesWithCompleteEntityFieldsAndStableOrder() {
    List<SysRole> roles = roleMapper.selectByUserId(7L);
    List<SysPermission> permissions = permissionMapper.selectByUserId(7L);

    assertEquals(List.of(1L, 2L), roles.stream().map(SysRole::getId).toList());
    assertEquals(List.of("ADMIN", "AUDITOR"), roles.stream().map(SysRole::getCode).toList());
    assertNotNull(roles.get(0).getCreatedAt());
    assertEquals(List.of(11L, 10L), permissions.stream().map(SysPermission::getId).toList());
    assertEquals(
        List.of("first:read", "later:read"),
        permissions.stream().map(SysPermission::getCode).toList());
    assertNotNull(permissions.get(0).getUpdatedAt());
  }

  @Test
  void serviceBuildsDeduplicatedSpringSecurityAuthorityCodes() {
    UserAuthorities authorities = authorityService.loadByUserId(7L);

    assertEquals(Set.of("ADMIN", "AUDITOR"), authorities.roleCodes());
    assertEquals(Set.of("first:read", "later:read"), authorities.permissionCodes());
    assertEquals(
        Set.of("ROLE_ADMIN", "ROLE_AUDITOR", "first:read", "later:read"),
        Set.copyOf(authorities.authorityCodes()));
  }
}
