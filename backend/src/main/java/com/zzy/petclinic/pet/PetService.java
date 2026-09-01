package com.zzy.petclinic.pet;

import com.zzy.petclinic.common.*;

public interface PetService {
  PageResponse<Pet> page(PageQuery query, Long ownerId);

  java.util.List<Pet> mine(Long userId);

  Pet get(Long id);

  Pet create(PetRequest request);

  Pet update(Long id, PetRequest request);

  void delete(Long id);
}
