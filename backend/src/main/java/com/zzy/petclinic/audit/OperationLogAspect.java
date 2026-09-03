package com.zzy.petclinic.audit;

import com.zzy.petclinic.rbac.authentication.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.*;

@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {
  private final OperationLogMapper mapper;

  @Around(
      "@annotation(org.springframework.web.bind.annotation.PostMapping) ||"
          + " @annotation(org.springframework.web.bind.annotation.PutMapping) ||"
          + " @annotation(org.springframework.web.bind.annotation.DeleteMapping) ||"
          + " @annotation(org.springframework.web.bind.annotation.PatchMapping)")
  public Object log(ProceedingJoinPoint p) throws Throwable {
    HttpServletRequest req =
        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    if (req.getRequestURI().startsWith("/api/auth/")) return p.proceed();
    long start = System.currentTimeMillis();
    String error = null;
    try {
      return p.proceed();
    } catch (Throwable e) {
      error = e.getMessage();
      throw e;
    } finally {
      try {
        OperationLog x = new OperationLog();
        Object principal =
            SecurityContextHolder.getContext().getAuthentication() == null
                ? null
                : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof AuthenticatedUser user) {
          x.setUserId(user.getId());
          x.setUsername(user.getUsername());
        }
        x.setModule(p.getSignature().getDeclaringType().getSimpleName());
        x.setOperation(p.getSignature().getName());
        x.setHttpMethod(req.getMethod());
        x.setRequestUri(req.getRequestURI());
        x.setResponseStatus(error == null ? 200 : 500);
        x.setDurationMs(System.currentTimeMillis() - start);
        x.setIpAddress(req.getRemoteAddr());
        x.setErrorMessage(error);
        x.setCreatedAt(LocalDateTime.now());
        x.setUpdatedAt(LocalDateTime.now());
        mapper.insert(x);
      } catch (Exception ignored) {
      }
    }
  }
}
