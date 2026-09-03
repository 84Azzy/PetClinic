package com.zzy.petclinic.rbac.authorization;

import com.zzy.petclinic.rbac.authentication.AuthenticatedUser;
import com.zzy.petclinic.rbac.authentication.CurrentUser;
import com.zzy.petclinic.rbac.system.SysPermission;
import com.zzy.petclinic.rbac.system.SysPermissionMapper;
import com.zzy.petclinic.rbac.system.SysRole;
import com.zzy.petclinic.rbac.system.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAuthorityServiceImpl implements UserAuthorityService{

    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final CurrentUser currentUser;
    /**
     * 装载指定用户的全部角色码和权限码。
     *
     * <p>实现提示：
     *
     * <ol>
     *   <li>调用 {@link SysRoleMapper#selectByUserId(Long)} 查询该用户已启用的角色。</li>
     *   <li>调用 {@link SysPermissionMapper#selectByUserId(Long)} 查询这些角色对应的已启用权限。</li>
     *   <li>分别提取 {@code SysRole.code} 和 {@code SysPermission.code}，组装成 {@link UserAuthorities}。</li>
     *   <li>用户不存在、未分配角色或没有权限时返回 {@link UserAuthorities#empty()}，不要返回 {@code null}。</li>
     * </ol>
     *
     * <p>查询 SQL 需要连接用户角色表和角色权限表，并过滤 {@code sys_role.status = 'ACTIVE'} 与
     * {@code sys_permission.status = 'ACTIVE'}。结果应去重，避免同一权限由多个角色授予时生成重复 authority。
     *
     * @param userId 已通过认证查询得到的用户编号
     * @return 可直接交给认证模块的角色码和权限码
     */
    @Override
    public UserAuthorities loadByUserId(Long userId) {
        AuthenticatedUser user = currentUser.require();
        if(user==null){
            return UserAuthorities.empty();
        }
        List<SysRole> roles = sysRoleMapper.selectByUserId(user.getId());
        if(roles.isEmpty()){
            return UserAuthorities.empty();
        }
        List<SysPermission> permissions = sysPermissionMapper.selectByUserId(user.getId());
        if(permissions.isEmpty()){
            return UserAuthorities.empty();
        }

        Set<String>roleCodes = roles.stream()
                .map(SysRole::getCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<String>permissionCodes = permissions.stream()
                .map(SysPermission::getCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return new UserAuthorities(roleCodes,permissionCodes);
    }
}
