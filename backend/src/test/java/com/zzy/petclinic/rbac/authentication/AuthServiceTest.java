package com.zzy.petclinic.rbac.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zzy.petclinic.common.BusinessException;
import com.zzy.petclinic.owner.Owner;
import com.zzy.petclinic.owner.OwnerMapper;
import com.zzy.petclinic.rbac.authentication.DTO.LoginRequest;
import com.zzy.petclinic.rbac.authentication.DTO.LoginResponse;
import com.zzy.petclinic.rbac.authentication.DTO.RegisterRequest;
import com.zzy.petclinic.rbac.authorization.UserAuthorities;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {
  private AuthenticationManager authenticationManager;
  private SysUserMapper userMapper;
  private OwnerMapper ownerMapper;
  private PasswordEncoder passwordEncoder;
  private AuthService service;
  private AuthenticatedUser user;

  @BeforeEach
  void setUp() {
    authenticationManager = mock(AuthenticationManager.class);
    userMapper = mock(SysUserMapper.class);
    ownerMapper = mock(OwnerMapper.class);
    passwordEncoder = mock(PasswordEncoder.class);
    JwtTokenProvider tokenProvider =
        new JwtTokenProvider("TestSecretKeyMustBeAtLeastThirtyTwoBytesLongForPetClinic", 60);
    service =
        new AuthService(
            authenticationManager,
            tokenProvider,
            userMapper,
            ownerMapper,
            passwordEncoder);

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

  @Test
  void registerCreatesOwnerAccountAndReturnsJwt() {
    RegisterRequest request =
        new RegisterRequest(
            "owner_test",
            "123456",
            "123456",
            "测试主人",
            "13812345678",
            "owner@example.com",
            "上海市浦东新区");

    when(userMapper.selectActiveRoleIdByCode("OWNER")).thenReturn(3L);
    when(passwordEncoder.encode("123456")).thenReturn("encoded-password");
    doAnswer(
            invocation -> {
              SysUser inserted = invocation.getArgument(0);
              inserted.setId(9L);
              return 1;
            })
        .when(userMapper)
        .insert(any(SysUser.class));
    when(userMapper.insertUserRole(9L, 3L)).thenReturn(1);
    when(ownerMapper.insert(any(Owner.class))).thenReturn(1);

    SysUser registered = new SysUser();
    registered.setId(9L);
    registered.setUsername("owner_test");
    registered.setPasswordHash("encoded-password");
    registered.setDisplayName("测试主人");
    registered.setPhone("13812345678");
    registered.setEmail("owner@example.com");
    registered.setAccountType("OWNER");
    registered.setStatus("ACTIVE");
    registered.setTokenVersion(1);
    AuthenticatedUser registeredPrincipal =
        new AuthenticatedUser(registered, new UserAuthorities(Set.of("OWNER"), Set.of()));
    when(authenticationManager.authenticate(any()))
        .thenReturn(
            new UsernamePasswordAuthenticationToken(
                registeredPrincipal, null, registeredPrincipal.getAuthorities()));

    LoginResponse response = service.register(request);

    assertEquals("owner_test", response.user().username());
    assertEquals("OWNER", response.user().accountType());
    assertFalse(response.token().isBlank());
    verify(userMapper).insertUserRole(9L, 3L);

    ArgumentCaptor<Owner> ownerCaptor = ArgumentCaptor.forClass(Owner.class);
    verify(ownerMapper).insert(ownerCaptor.capture());
    assertEquals(9L, ownerCaptor.getValue().getUserId());
    assertEquals("测试主人", ownerCaptor.getValue().getName());
  }

  @Test
  void registerRejectsMismatchedPasswords() {
    RegisterRequest request =
        new RegisterRequest(
            "owner_test",
            "123456",
            "654321",
            "测试主人",
            "13812345678",
            null,
            null);

    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.register(request));

    assertEquals("两次输入的密码不一致", exception.getMessage());
  }
}
