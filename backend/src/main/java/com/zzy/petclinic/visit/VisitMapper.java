package com.zzy.petclinic.visit;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface VisitMapper extends BaseMapper<Visit> {
  Visit selectByRequestId(String requestId);

  List<Visit> selectMine(Long userId);

  int claimSlot(@Param("slotId") Long slotId);

  int releaseSlot(@Param("slotId") Long slotId);
}
