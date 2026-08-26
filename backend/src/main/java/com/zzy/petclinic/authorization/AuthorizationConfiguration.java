package com.zzy.petclinic.authorization;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 权限模块的默认装配；在 RBAC 尚未实现时返回空权限。 */
@Configuration
public class AuthorizationConfiguration {
  @Bean
  @ConditionalOnMissingBean(UserAuthorityService.class)
  UserAuthorityService emptyUserAuthorityService() {
    return userId -> UserAuthorities.empty();
  }
}
