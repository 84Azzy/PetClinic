package com.zzy.petclinic.rbac.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zzy.petclinic.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * 403异常，其实是死代码，
 * 在配置过滤器链时根本没有配置`.requestMatchers("/api/admin/**").hasRole("ADMIN")`这样的代码
 * 即给url加上带角色的规则
 * 项目403异常只会出现在@PreAuthorize注解下
 * 但这条路会被GlobalExceptionHandler捕获，不会执行RestAccessDeniedHandler
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {
  private final ObjectMapper objectMapper;

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setCharacterEncoding("UTF-8");
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getWriter(), ApiResponse.error(403, "没有权限执行此操作"));
  }
}
