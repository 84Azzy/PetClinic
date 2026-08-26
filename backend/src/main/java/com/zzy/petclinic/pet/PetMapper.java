package com.zzy.petclinic.pet;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface PetMapper extends BaseMapper<Pet> {
  List<Pet> selectMine(@Param("userId") Long userId);
}
