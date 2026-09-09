package com.zzy.petclinic.rbac.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzy.petclinic.rbac.system.dataObject.SysPermission;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 权限表 Mapper；基础增删改查由 MyBatis-Plus 提供，自定义授权查询写在同名 XML 中。 */
public interface SysPermissionMapper extends BaseMapper<SysPermission> {
  /**
   * 查询用户通过有效角色获得的有效权限。
   *
   * <p>XML 提示：按 sys_user_role → sys_role → sys_role_permission → sys_permission 的顺序连接；过滤 user_id、角色 ACTIVE 和权限
   * ACTIVE；使用 DISTINCT 消除多个角色授予同一权限造成的重复；按 sort_order、id 排序。
   *
   * @param userId 用户编号
   * @return 用户可用权限的扁平列表，组树由 Service 完成
   */
  List<SysPermission> selectByUserId(@Param("userId")Long userId);

  /**
   * 查询某节点的直接子节点数量
   * @param permissionId
   * @return
   */
  long countChildrenByParentId(@Param("permissionId")Long permissionId);

  /**
   * 删除角色与指定权限之间的关连
   * @param permissionId
   * @return
   */
  int deleteRoleRelationsByPermissionId(@Param("permissionId")Long permissionId);
}
