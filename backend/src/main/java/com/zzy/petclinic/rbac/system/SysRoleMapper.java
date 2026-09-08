package com.zzy.petclinic.rbac.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

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
  // 不恰当：依赖编译器保留单参数名称，不利于 XML 重构和定位绑定错误。
  // List<SysRole> selectByUserId(Long userId);
  List<SysRole> selectByUserId(@Param("userId") Long userId);

  /**
   * 根据roleId查询是否还有用户关联角色
   * @param roleId
   * @return
   */
  // 不恰当：COUNT(*) 不会返回 null，包装类型 Long 没有必要。
  // Long countUsersByRoleId(@Param("roleId") Long roleId);
  long countUsersByRoleId(@Param("roleId") Long roleId);

  // 不恰当：删除的是多条权限关系，方法名使用单数容易误解为只删一条。
  // int deletePermissionByRoleId(@Param("roleId") Long roleId);
  int deletePermissionsByRoleId(@Param("roleId") Long roleId);

  // 不恰当：permissionList 只说明容器类型，没有准确表达其中保存的是权限 ID。
  // int insertRolePermissions(Long roleId, List<Long> permissionList);
  int insertRolePermissions(
      @Param("roleId") Long roleId, @Param("permissionIds") List<Long> permissionIds);
}
