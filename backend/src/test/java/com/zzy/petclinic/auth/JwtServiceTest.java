package com.zzy.petclinic.auth;

import static org.junit.jupiter.api.Assertions.*;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.*;

class JwtServiceTest {
  private JwtService service;
  private SysUser user;

  @BeforeEach
  void setUp() {
    service = new JwtService("TestSecretKeyMustBeAtLeastThirtyTwoBytesLongForPetClinic", 60);
    user = new SysUser();
    user.setId(7L);
    user.setUsername("owner");
    user.setDisplayName("宠主");
    user.setAccountType("OWNER");
    user.setTokenVersion(2);
  }

  @Test
  void tokenContainsIdentity() {
    Claims c = service.parse(service.issue(user));
    assertEquals("7", c.getSubject());
    assertEquals("owner", c.get("username"));
  }

  @Test
  void tokenContainsVersion() {
    assertEquals(2, service.parse(service.issue(user)).get("tokenVersion", Integer.class));
  }

  @Test
  void reportsConfiguredLifetime() {
    assertEquals(3600, service.expiresInSeconds());
  }

  @Test
  void rejectsMalformedToken() {
    assertThrows(Exception.class, () -> service.parse("bad-token"));
  }
}
