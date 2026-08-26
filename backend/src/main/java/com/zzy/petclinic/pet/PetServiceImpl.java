package com.zzy.petclinic.pet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzy.petclinic.common.PageQuery;
import com.zzy.petclinic.common.PageResponse;
import com.zzy.petclinic.owner.Owner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PetServiceImpl implements PetService{

    private final PetMapper petMapper;

    @Override
    public PageResponse<Pet> page(PageQuery query, Long ownerId) {
        Page<Pet> page = new Page<>(query.pageValue(), query.sizeValue());
        LambdaQueryWrapper<Pet> wrapper = new LambdaQueryWrapper<>();
        if(ownerId!=null){
            wrapper.eq(Pet::getOwnerId,ownerId);
        }
        if(query.keyword()!=null){
            wrapper.like(Pet::getName,query.keyword());
        }
        Page<Pet> list = petMapper.selectPage(page,wrapper);
        return PageResponse.of(list);
    }

    @Override
    public List<Pet> mine() {
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
