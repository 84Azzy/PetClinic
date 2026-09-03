package com.zzy.petclinic.rbac.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzy.petclinic.rbac.authentication.SysUser;
import java.util.List;

/**
 * 用户后台管理所需的自定义 SQL。
 *
 * <p>分页列表和总数查询必须使用完全相同的筛选条件。角色替换涉及两次写操作，应由 Service 开启事务；不要为了在一个 Mapper 方法里执行多条 SQL
 * 而开启 JDBC {@code allowMultiQueries}，更推荐把删除旧关系和批量插入拆成两个 Mapper 方法。
 */
@org.apache.ibatis.annotations.Mapper
public interface SysUserAdminMapper extends BaseMapper<SysUser> {
  /**
   * 查询一页用户。
   *
   * <p>XML 提示：从 sys_user 查询；keyword 有内容时模糊匹配 username、display_name、phone、email；status 有内容时精确匹配；按 id
   * 倒序，最后使用 LIMIT offset,size。列名应显式列出并排除 password_hash，避免密码哈希经 Controller 泄露。
   */
  List<SysUser> selectPageUsers(String keyword, String status, long offset, long size);

  /** 与 {@link #selectPageUsers} 使用相同条件统计总记录数，但不添加排序和 LIMIT。 */
  long countUsers(String keyword, String status);

  /**
   * 全量替换用户角色关系。
   *
   * <p>实现前建议把此方法拆成 {@code deleteRolesByUserId(userId)} 与批量 {@code insertUserRoles(userId, roleIds)}，再由 Service 的事务依次
   * 调用。roleIds 为空时只删除旧关系，不生成空的 IN 或 VALUES 语句。
   */
  void deleteRolesByUserId(Long userId);
  void insertUserRoles(Long userId,List<Long>roleIds);
}
