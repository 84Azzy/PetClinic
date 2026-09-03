package com.zzy.petclinic.rbac.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zzy.petclinic.rbac.authorization.UserAuthorities;
import com.zzy.petclinic.rbac.authentication.AuthenticatedUser;
import com.zzy.petclinic.rbac.authentication.JwtTokenProvider;
import com.zzy.petclinic.rbac.authentication.SysUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {
  private JwtTokenProvider provider;
  private SysUser entity;

  @BeforeEach
  void setUp() {
    provider =
        new JwtTokenProvider("TestSecretKeyMustBeAtLeastThirtyTwoBytesLongForPetClinic", 60);
    entity = new SysUser();
    entity.setId(7L);
    entity.setUsername("owner");
    entity.setPasswordHash("encoded");
    entity.setStatus("ACTIVE");
    entity.setTokenVersion(2);
  }

  @Test
  void tokenContainsIdentityAndVersion() {
    AuthenticatedUser user = new AuthenticatedUser(entity, UserAuthorities.empty());
    String token = provider.generate(user);

    assertEquals("owner", provider.getUsername(token));
    assertEquals(7, provider.parse(token).get("uid", Integer.class));
    assertTrue(provider.isValid(token, user));
  }

  @Test
  void tokenVersionInvalidatesExistingToken() {
    AuthenticatedUser original = new AuthenticatedUser(entity, UserAuthorities.empty());
    String token = provider.generate(original);
    entity.setTokenVersion(3);

    assertFalse(
        provider.isValid(token, new AuthenticatedUser(entity, UserAuthorities.empty())));
  }

  @Test
  void rejectsMalformedToken() {
    assertThrows(Exception.class, () -> provider.getUsername("bad-token"));
  }
}
