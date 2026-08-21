package com.zzy.petclinic.rbac;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;

public interface SysPermissionMapper extends BaseMapper<SysPermission> {
  List<SysPermission> selectByUserId(Long userId);
}
