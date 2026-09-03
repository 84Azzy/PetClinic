package com.zzy.petclinic.rbac.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zzy.petclinic.rbac.authorization.UserAuthorities;
import com.zzy.petclinic.rbac.authorization.UserAuthorityService;
import java.util.Set;

import com.zzy.petclinic.rbac.authentication.AuthenticatedUser;
import com.zzy.petclinic.rbac.authentication.DatabaseUserDetailsService;
import com.zzy.petclinic.rbac.authentication.SysUser;
import com.zzy.petclinic.rbac.authentication.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class DatabaseUserDetailsServiceTest {
  private SysUserMapper userMapper;
  private UserAuthorityService authorityService;
  private DatabaseUserDetailsService service;

  @BeforeEach
  void setUp() {
    userMapper = mock(SysUserMapper.class);
    authorityService = mock(UserAuthorityService.class);
    service = new DatabaseUserDetailsService(userMapper, authorityService);
  }

  @Test
  void adaptsDatabaseUserAndLoadedAuthorities() {
    SysUser entity = new SysUser();
    entity.setId(1L);
    entity.setUsername("admin");
    entity.setPasswordHash("encoded");
    entity.setStatus("ACTIVE");
    entity.setTokenVersion(1);
    when(userMapper.selectOne(any())).thenReturn(entity);
    when(authorityService.loadByUserId(1L))
        .thenReturn(new UserAuthorities(Set.of("ADMIN"), Set.of("system:manage")));

    AuthenticatedUser user = (AuthenticatedUser) service.loadUserByUsername("admin");

    assertEquals(
        Set.of("ROLE_ADMIN", "system:manage"),
        user.getAuthorities().stream().map(authority -> authority.getAuthority()).collect(java.util.stream.Collectors.toSet()));
  }

  @Test
  void rejectsUnknownUsername() {
    assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("nobody"));
  }
}
