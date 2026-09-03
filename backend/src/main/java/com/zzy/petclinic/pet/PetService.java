package com.zzy.petclinic.pet;

import com.zzy.petclinic.common.*;

public interface PetService {
  PageResponse<Pet> page(PageQuery query, Long ownerId);

  // 不恰当：调用者可以传入任意 userId，破坏“我的宠物”边界。
  // java.util.List<Pet> mine(Long userId);
  java.util.List<Pet> mine();

  Pet get(Long id);

  Pet create(PetRequest request);

  Pet update(Long id, PetRequest request);

  void delete(Long id);
}
