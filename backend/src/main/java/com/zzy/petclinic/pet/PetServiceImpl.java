package com.zzy.petclinic.pet;

import com.zzy.petclinic.common.PageQuery;
import com.zzy.petclinic.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PetServiceImpl implements PetService{

    private PetMapper petMapper;

    @Override
    public PageResponse<Pet> page(PageQuery query, Long ownerId) {
        return null;
    }

    @Override
    public List<Pet> mine(Long user_id) {

        return List.of();
    }

    @Override
    public Pet get(Long id) {
        return null;
    }

    @Override
    public Pet create(PetRequest request) {
        return null;
    }

    @Override
    public Pet update(Long id, PetRequest request) {
        return null;
    }

    @Override
    public void delete(Long id) {

    }
}
