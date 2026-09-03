package com.zzy.petclinic.rbac.authorization;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 权限模块的防御性默认装配。
 *
 * <p>当前 Bean 只在应用没有提供 {@link UserAuthorityService} 实现时启用，使认证模块仍能以空权限安全启动。正常运行时
 * {@link UserAuthorityServiceImpl} 存在，{@link ConditionalOnMissingBean} 会让本兜底 Bean 自动退出。
 */
@Configuration
public class AuthorizationConfiguration {
  @Bean
  @ConditionalOnMissingBean(UserAuthorityService.class)
  UserAuthorityService emptyUserAuthorityService() {
    // 兜底实现保持 fail-closed：缺少正式授权服务时不授予任何角色或权限。
    return userId -> UserAuthorities.empty();
  }
}
