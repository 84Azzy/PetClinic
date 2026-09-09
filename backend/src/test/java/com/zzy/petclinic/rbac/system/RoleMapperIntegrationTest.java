package com.zzy.petclinic.rbac.system;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import com.zzy.petclinic.rbac.system.dataObject.SysRole;
import com.zzy.petclinic.rbac.system.mapper.SysRoleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

/** 使用 H2 执行真实角色 SQL，验收 roleId 方向、关系清理和批量权限参数绑定。 */
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
      "CREATE TABLE sys_user_role (user_id BIGINT, role_id BIGINT, PRIMARY KEY (user_id, role_id))",
      "CREATE TABLE sys_role_permission (role_id BIGINT, permission_id BIGINT,"
          + " PRIMARY KEY (role_id, permission_id))",
      "INSERT INTO sys_role VALUES"
          + " (1, 'ADMIN', '管理员', NULL, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),"
          + " (2, 'OLD', '停用角色', NULL, 'INACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
      "INSERT INTO sys_permission VALUES"
          + " (10, NULL, 'pet:read', '查看宠物', 'API', NULL, NULL, 1, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),"
          + " (11, NULL, 'pet:update', '修改宠物', 'API', NULL, NULL, 2, 'INACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
      "INSERT INTO sys_user_role VALUES (7, 1), (8, 1), (7, 2)",
      "INSERT INTO sys_role_permission VALUES (1, 10), (2, 11)"
    })
class RoleMapperIntegrationTest {
  @Autowired private SysRoleMapper mapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void selectByUserIdReturnsOnlyActiveRolesWhileDeleteCheckUsesRoleId() {
    assertEquals(List.of(1L), mapper.selectByUserId(7L).stream().map(SysRole::getId).toList());
    assertEquals(2L, mapper.countUsersByRoleId(1L));
    assertEquals(1L, mapper.countUsersByRoleId(2L));
  }

  @Test
  void permissionRelationMethodsDeleteAllAndBindPermissionIds() {
    assertEquals(1, mapper.deletePermissionsByRoleId(1L));
    assertEquals(2, mapper.insertRolePermissions(1L, List.of(10L, 11L)));

    List<Long> permissionIds =
        jdbcTemplate.queryForList(
            "select permission_id from sys_role_permission where role_id = 1 order by permission_id",
            Long.class);
    assertEquals(List.of(10L, 11L), permissionIds);
  }
}
