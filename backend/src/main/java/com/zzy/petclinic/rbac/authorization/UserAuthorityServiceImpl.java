package com.zzy.petclinic.rbac.authorization;

import com.zzy.petclinic.rbac.system.SysPermission;
import com.zzy.petclinic.rbac.system.SysPermissionMapper;
import com.zzy.petclinic.rbac.system.SysRole;
import com.zzy.petclinic.rbac.system.SysRoleMapper;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAuthorityServiceImpl implements UserAuthorityService {

  private final SysRoleMapper sysRoleMapper;
  private final SysPermissionMapper sysPermissionMapper;

  // 不恰当：权限正是在登录过程中装载的，此时 SecurityContext 里还没有当前用户；同时这会忽略 userId 入参。
  // private final CurrentUser currentUser;

  @Override
  public UserAuthorities loadByUserId(Long userId) {
    // 不恰当：require() 未登录时会直接抛 401，后面的 null 判断永远不会成立。
    // AuthenticatedUser user = currentUser.require();
    // if (user == null) {
    //   return UserAuthorities.empty();
    // }
    // 正确：本方法是认证模块的底层装载端口，只使用已经查询确认过的用户编号。
    if (userId == null) {
      return UserAuthorities.empty();
    }

    // 不恰当：忽略方法入参，改查尚未写入认证上下文的 user.getId()。
    // List<SysRole> roles = sysRoleMapper.selectByUserId(user.getId());
    List<SysRole> roles = sysRoleMapper.selectByUserId(userId);
    if (roles.isEmpty()) {
      return UserAuthorities.empty();
    }

    // List<SysPermission> permissions = sysPermissionMapper.selectByUserId(user.getId());
    List<SysPermission> permissions = sysPermissionMapper.selectByUserId(userId);

    // 不恰当：没有细粒度权限不代表没有角色，直接返回 empty() 会丢失 ROLE_xxx。
    // if (permissions.isEmpty()) {
    //   return UserAuthorities.empty();
    // }

    Set<String> roleCodes =
        roles.stream()
            .map(SysRole::getCode)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    Set<String> permissionCodes =
        permissions.stream()
            .map(SysPermission::getCode)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    return new UserAuthorities(roleCodes, permissionCodes);
  }
}
