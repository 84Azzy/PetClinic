package com.zzy.petclinic.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.zzy.petclinic.common.BusinessException;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthServiceTest {
  private AuthAccountMapper mapper;
  private AuthService service;
  private SysUser user;

  @BeforeEach
  void setUp() {
    mapper = mock(AuthAccountMapper.class);
    var encoder = new BCryptPasswordEncoder();
    var jwt = new JwtService("TestSecretKeyMustBeAtLeastThirtyTwoBytesLongForPetClinic", 60);
    service = new AuthService(mapper, encoder, jwt);
    user = new SysUser();
    user.setId(1L);
    user.setUsername("admin");
    user.setDisplayName("管理员");
    user.setPasswordHash(encoder.encode("123456"));
    user.setAccountType("ADMIN");
    user.setStatus("ACTIVE");
    user.setTokenVersion(1);
  }

  @Test
  void loginReturnsJwt() {
    when(mapper.selectOne(any())).thenReturn(user);
    assertFalse(service.login(new LoginRequest("admin", "123456")).token().isBlank());
  }

  @Test
  void wrongPasswordIsUnauthorized() {
    when(mapper.selectOne(any())).thenReturn(user);
    assertEquals(
        401,
        assertThrows(BusinessException.class, () -> service.login(new LoginRequest("admin", "bad")))
            .getStatus()
            .value());
  }

  @Test
  void missingUserIsUnauthorized() {
    assertThrows(BusinessException.class, () -> service.login(new LoginRequest("nobody", "bad")));
  }

  @Test
  void disabledUserIsForbidden() {
    user.setStatus("INACTIVE");
    when(mapper.selectOne(any())).thenReturn(user);
    assertEquals(
        403,
        assertThrows(
                BusinessException.class, () -> service.login(new LoginRequest("admin", "123456")))
            .getStatus()
            .value());
  }

  @Test
  void profileExcludesPassword() {
    assertEquals("admin", service.profile(user).username());
  }
}
