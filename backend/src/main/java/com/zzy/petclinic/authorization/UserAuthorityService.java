package com.zzy.petclinic.authorization;

/**
 * authentication 与 RBAC 实现之间的端口。
 *
 * <p>后续只需提供一个该接口的 Spring Bean，认证体系便会自动装载角色和权限。
 */
@FunctionalInterface
public interface UserAuthorityService {
  UserAuthorities loadByUserId(Long userId);
}
