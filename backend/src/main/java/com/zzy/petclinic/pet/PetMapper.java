package com.zzy.petclinic.pet;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;

public interface PetMapper extends BaseMapper<Pet> {
  List<Pet> selectMine(Long userId);
}
