package com.zzy.petclinic.rbac;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;

public interface SysRoleMapper extends BaseMapper<SysRole> {
  List<SysRole> selectByUserId(Long userId);
}
