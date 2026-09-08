package com.zzy.petclinic.rbac.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 角色表 Mapper；基础增删改查由 MyBatis-Plus 提供，自定义方法写在同名 XML 中。 */
public interface SysRoleMapper extends BaseMapper<SysRole> {
  /**
   * 查询用户拥有的有效角色。
   *
   * <p>XML 提示：sys_user_role 连接 sys_role，按 user_id 过滤，同时限定角色状态 ACTIVE；使用 DISTINCT 去重并稳定排序。不要根据
   * sys_user.account_type 伪造角色，授权关系以 sys_user_role 为准。
   *
   * @param userId 用户编号
   * @return 已启用角色列表
   */
  List<SysRole> selectByUserId(Long userId);

  /**
   * 根据roleId查询是否还有用户关联角色
   * @param roleId
   * @return
   */
  Long countUsersByRoleId(@Param("roleId")Long roleId);

  int deletePermissionByRoleId(@Param("roleId") Long roleId);

  int insertRolePermissions(@Param("roleId") Long roleId,@Param("permissionList")List<Long> permissionList);
}
