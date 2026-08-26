package com.zzy.petclinic.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;

public interface SysPermissionMapper extends BaseMapper<SysPermission> {
  List<SysPermission> selectByUserId(Long userId);
}
