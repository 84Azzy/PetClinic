package com.zzy.petclinic.rbac.authorization;

import com.zzy.petclinic.rbac.system.mapper.SysPermissionMapper;
import com.zzy.petclinic.rbac.system.mapper.SysRoleMapper;

/**
 * authentication 与 RBAC 实现之间的端口。
 *
 * <p>后续只需提供一个该接口的 Spring Bean，认证体系便会自动装载角色和权限。
 */
@FunctionalInterface
public interface UserAuthorityService {
  /**
   * 装载指定用户的全部角色码和权限码。
   *
   * <p>实现提示：
   *
   * <ol>
   *   <li>调用 {@link SysRoleMapper#selectByUserId(Long)} 查询该用户已启用的角色。</li>
   *   <li>调用 {@link SysPermissionMapper#selectByUserId(Long)} 查询这些角色对应的已启用权限。</li>
   *   <li>分别提取 {@code SysRole.code} 和 {@code SysPermission.code}，组装成 {@link UserAuthorities}。</li>
   *   <li>用户编号无效或未分配角色时返回 {@link UserAuthorities#empty()}，不要返回 {@code null}。</li>
   *   <li>用户有角色但没有细粒度权限时仍须保留角色码。</li>
   * </ol>
   *
   * <p>查询 SQL 需要连接用户角色表和角色权限表，并过滤 {@code sys_role.status = 'ACTIVE'} 与
   * {@code sys_permission.status = 'ACTIVE'}。结果应去重，避免同一权限由多个角色授予时生成重复 authority。
   *
   * @param userId 已通过认证查询得到的用户编号
   * @return 可直接交给认证模块的角色码和权限码
   */
  UserAuthorities loadByUserId(Long userId);
}
