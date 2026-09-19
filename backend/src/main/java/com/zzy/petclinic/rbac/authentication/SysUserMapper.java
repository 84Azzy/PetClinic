package com.zzy.petclinic.rbac.authentication;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SysUserMapper extends BaseMapper<SysUser> {

  @Select("""
      select id
      from sys_role
      where code = #{code}
        and status = 'ACTIVE'
      limit 1
      """)
  Long selectActiveRoleIdByCode(@Param("code") String code);

  @Insert("""
      insert into sys_user_role(user_id, role_id)
      values(#{userId}, #{roleId})
      """)
  int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
