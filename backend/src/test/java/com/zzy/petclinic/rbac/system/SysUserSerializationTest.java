package com.zzy.petclinic.rbac.system;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zzy.petclinic.rbac.authentication.SysUser;
import org.junit.jupiter.api.Test;

class SysUserSerializationTest {
  @Test
  void passwordHashIsNeverSerialized() throws Exception {
    SysUser user = new SysUser();
    user.setUsername("admin");
    user.setPasswordHash("sensitive-hash");

    String json = new ObjectMapper().writeValueAsString(user);

    assertTrue(json.contains("admin"));
    assertFalse(json.contains("passwordHash"));
    assertFalse(json.contains("sensitive-hash"));
  }
}
