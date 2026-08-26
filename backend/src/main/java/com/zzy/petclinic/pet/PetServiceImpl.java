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
    public List<Pet> mine(Long ownId) {
        List<Pet> list = petMapper.selectList(new LambdaQueryWrapper<Pet>()
                .eq(Pet::getOwnerId,ownId)
                .orderByAsc(Pet::getId));
        return list;
    }

    @Override
    public Pet get(Long id) {
        return petMapper.selectById(id);
    }

    @Override
    public Pet create(PetRequest r) {
        Pet pet = new Pet();
        pet.setOwnerId(r.ownerId());
        pet.setTypeId(r.typeId());
        pet.setName(r.name());
        if(r.gender() != null){
            pet.setGender(r.gender());
        }
        if(r.breed() != null){
            pet.setBreed(r.breed());
        }
        if(r.birthDate() != null){
            pet.setBirthDate(r.birthDate());
        }
        if(r.color() != null){
            pet.setColor(r.color());
        }
        if(r.microchipNo() != null){
            pet.setMicrochipNo(r.microchipNo());
        }
        if(r.allergies() != null){
            pet.setAllergies(r.allergies());
        }
        if(r.photoUrl() != null){
            pet.setPhotoUrl(r.photoUrl());
        }
        petMapper.insert(pet);
        return pet;
    }

    @Override
    public Pet update(Long id, PetRequest r) {
        Pet pet = petMapper.selectById(id);
        if(r.ownerId() != null){
            pet.setOwnerId(r.ownerId());
        }
        if(r.typeId() != null){
            pet.setTypeId(r.typeId());
        }
        if(r.name() != null){
            pet.setName(r.name());
        }
        if(r.gender() != null){
            pet.setGender(r.gender());
        }
        if(r.breed() != null){
            pet.setBreed(r.breed());
        }
        if(r.birthDate() != null){
            pet.setBirthDate(r.birthDate());
        }
        if(r.color() != null){
            pet.setColor(r.color());
        }
        if(r.microchipNo() != null){
            pet.setMicrochipNo(r.microchipNo());
        }
        if(r.allergies() != null){
            pet.setAllergies(r.allergies());
        }
        if(r.photoUrl() != null){
            pet.setPhotoUrl(r.photoUrl());
        }
        petMapper.updateById(pet);
        return pet;
    }

    @Override
    public void delete(Long id) {
        Pet pet = petMapper.selectById(id);
        pet.setStatus("UNACTIVE");
        petMapper.updateById(pet);
    }
}
