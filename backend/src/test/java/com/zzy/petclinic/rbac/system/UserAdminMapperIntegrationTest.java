package com.zzy.petclinic.rbac.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.zzy.petclinic.rbac.authentication.SysUser;
import java.util.List;

import com.zzy.petclinic.rbac.system.mapper.SysUserAdminMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

/** 使用 H2 执行真实用户管理 SQL，避免动态 where、列名和集合参数错误被单元测试掩盖。 */
@SpringBootTest
@Sql(
    statements = {
      "DROP TABLE IF EXISTS sys_user_role",
      "DROP TABLE IF EXISTS sys_role",
      "DROP TABLE IF EXISTS sys_user",
      "CREATE TABLE sys_user (id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) UNIQUE NOT NULL,"
          + " password_hash VARCHAR(100) NOT NULL, display_name VARCHAR(50) NOT NULL, phone VARCHAR(20),"
          + " email VARCHAR(100), account_type VARCHAR(20) NOT NULL, status VARCHAR(20) NOT NULL,"
          + " token_version INT NOT NULL, created_at TIMESTAMP, updated_at TIMESTAMP)",
      "CREATE TABLE sys_role (id BIGINT PRIMARY KEY, code VARCHAR(50), name VARCHAR(50),"
          + " description VARCHAR(255), status VARCHAR(20), created_at TIMESTAMP, updated_at TIMESTAMP)",
      "CREATE TABLE sys_user_role (user_id BIGINT, role_id BIGINT, PRIMARY KEY (user_id, role_id))",
      "INSERT INTO sys_user VALUES"
          + " (1, 'admin', 'hash-a', '管理员', '13800000001', 'a@example.com', 'ADMIN', 'ACTIVE', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),"
          + " (2, 'alice', 'hash-b', '爱丽丝', '13800000002', 'b@example.com', 'STAFF', 'INACTIVE', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),"
          + " (3, 'bob', 'hash-c', '鲍勃', NULL, NULL, 'OWNER', 'ACTIVE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
      "INSERT INTO sys_role VALUES"
          + " (10, 'ADMIN', '管理员', NULL, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),"
          + " (11, 'AUDITOR', '审计员', NULL, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
      "INSERT INTO sys_user_role VALUES (1, 10)"
    })
class UserAdminMapperIntegrationTest {
  @Autowired private SysUserAdminMapper mapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void pageWithoutFiltersIsValidAndNeverSelectsPasswordHash() {
    List<SysUser> users = mapper.selectPageUsers(null, null, 0, 2);

    assertEquals(List.of(3L, 2L), users.stream().map(SysUser::getId).toList());
    assertEquals(3L, mapper.countUsers(null, null));
    assertEquals(1, users.get(0).getTokenVersion());
    assertNull(users.get(0).getPasswordHash());
  }

  @Test
  void keywordAndStatusUseTheSameFiltersForPageAndCount() {
    List<SysUser> users = mapper.selectPageUsers("13800000001", "ACTIVE", 0, 10);

    assertEquals(List.of(1L), users.stream().map(SysUser::getId).toList());
    assertEquals(1L, mapper.countUsers("13800000001", "ACTIVE"));
  }

  @Test
  void roleReplacementSqlUsesDeclaredCollectionParameter() {
    assertEquals(1, mapper.deleteRolesByUserId(1L));
    assertEquals(2, mapper.insertUserRoles(1L, List.of(10L, 11L)));

    List<Long> roleIds =
        jdbcTemplate.queryForList(
            "select role_id from sys_user_role where user_id = 1 order by role_id", Long.class);
    assertEquals(List.of(10L, 11L), roleIds);
  }
}
