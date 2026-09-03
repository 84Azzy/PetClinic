package com.zzy.petclinic.rbac.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zzy.petclinic.rbac.authentication.AuthService;
import com.zzy.petclinic.rbac.authentication.AuthenticatedUser;
import com.zzy.petclinic.rbac.authentication.DTO.LoginRequest;
import com.zzy.petclinic.rbac.authentication.DTO.LoginResponse;
import com.zzy.petclinic.rbac.authorization.UserAuthorities;
import com.zzy.petclinic.rbac.authentication.JwtTokenProvider;
import com.zzy.petclinic.rbac.authentication.SysUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class AuthServiceTest {
  private AuthenticationManager authenticationManager;
  private AuthService service;
  private AuthenticatedUser user;

  @BeforeEach
  void setUp() {
    authenticationManager = mock(AuthenticationManager.class);
    JwtTokenProvider tokenProvider =
        new JwtTokenProvider("TestSecretKeyMustBeAtLeastThirtyTwoBytesLongForPetClinic", 60);
    service = new AuthService(authenticationManager, tokenProvider);

    SysUser entity = new SysUser();
    entity.setId(1L);
    entity.setUsername("admin");
    entity.setPasswordHash("encoded-password");
    entity.setDisplayName("管理员");
    entity.setAccountType("ADMIN");
    entity.setStatus("ACTIVE");
    entity.setTokenVersion(1);
    user = new AuthenticatedUser(entity, UserAuthorities.empty());
  }

  @Test
  void loginDelegatesToAuthenticationManagerAndReturnsJwt() {
    when(authenticationManager.authenticate(any()))
        .thenReturn(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));

    LoginResponse response = service.login(new LoginRequest("admin", "123456"));

    assertFalse(response.token().isBlank());
    assertEquals("admin", response.user().username());
    assertEquals("Bearer", response.tokenType());
  }
}
