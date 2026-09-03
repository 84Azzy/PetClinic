package com.zzy.petclinic.rbac.authorization;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 权限模块的临时默认装配。
 *
 * <p>当前 Bean 只用于保证 RBAC 未完成时认证模块仍能启动，所有登录用户都会得到空权限。完成授权模块后，新增一个实现
 * {@link UserAuthorityService} 的 Spring Bean 即可；{@link ConditionalOnMissingBean} 会让本兜底 Bean 自动退出，无需删除此配置。
 */
@Configuration
public class AuthorizationConfiguration {
  @Bean
  @ConditionalOnMissingBean(UserAuthorityService.class)
  UserAuthorityService emptyUserAuthorityService() {
    // TODO(授权模块)：不要在这里写数据库查询；应新建独立实现类并注入角色、权限 Mapper。
    return userId -> UserAuthorities.empty();
  }
}
