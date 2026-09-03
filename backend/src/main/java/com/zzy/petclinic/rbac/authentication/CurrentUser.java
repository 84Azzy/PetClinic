package com.zzy.petclinic.rbac.authentication;

import com.zzy.petclinic.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
  public AuthenticatedUser require() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
      throw new BusinessException(HttpStatus.UNAUTHORIZED, "请先登录");
    }
    return principal;
  }

  public Long id() {
    return require().getId();
  }
}
