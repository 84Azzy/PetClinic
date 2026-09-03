package com.zzy.petclinic.rbac.authentication;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.zzy.petclinic.rbac.authorization.UserAuthorityService;
import com.zzy.petclinic.rbac.authentication.DatabaseUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;

@SpringBootTest
class AuthenticationContextTest {
  @Autowired private AuthenticationManager authenticationManager;
  @Autowired private UserDetailsService userDetailsService;
  @Autowired private UserAuthorityService userAuthorityService;

  @Test
  void authenticationChainIsFullyWired() {
    assertNotNull(authenticationManager);
    assertInstanceOf(DatabaseUserDetailsService.class, userDetailsService);
    assertNotNull(userAuthorityService);
  }
}
