package com.zzy.petclinic.rbac;

import com.zzy.petclinic.auth.SysUser;
import java.util.List;

@org.apache.ibatis.annotations.Mapper
public interface SysUserAdminMapper {
  List<SysUser> selectPageUsers(String keyword, String status, long offset, long size);

  long countUsers(String keyword, String status);

  void replaceRoles(Long userId, List<Long> roleIds);
}
