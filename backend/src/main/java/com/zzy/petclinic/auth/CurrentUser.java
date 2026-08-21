package com.zzy.petclinic.auth;

import com.zzy.petclinic.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
  public AccountPrincipal require() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof AccountPrincipal principal))
      throw new BusinessException(HttpStatus.UNAUTHORIZED, "请先登录");
    return principal;
  }

  public Long id() {
    return require().id();
  }
}
