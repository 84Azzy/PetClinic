package com.zzy.petclinic.pet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzy.petclinic.rbac.authentication.AuthenticatedUser;
import com.zzy.petclinic.rbac.authentication.CurrentUser;
import com.zzy.petclinic.common.BusinessException;
import com.zzy.petclinic.common.PageQuery;
import com.zzy.petclinic.common.PageResponse;
import com.zzy.petclinic.owner.Owner;
import com.zzy.petclinic.owner.OwnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PetServiceImpl implements PetService{

    private final PetMapper petMapper;
    private final CurrentUser currentUser;
    private final OwnerService ownerService;

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
        //状态不传默认为活跃
        String status = query.status()==null?"ACTIVE": query.status();
        wrapper.eq(Pet::getStatus,status);
        Page<Pet> list = petMapper.selectPage(page,wrapper);
        return PageResponse.of(list);
    }

    @Override
    public List<Pet> mine(Long userId) {
        return petMapper.selectMine(userId);
    }

    @Override
    public Pet get(Long id) {
        Pet pet = petMapper.selectById(id);
        //查询不存在的宠物返回 404
        if(pet==null){
            throw new BusinessException(HttpStatus.NOT_FOUND,"宠物不存在");
        }
        AuthenticatedUser user = currentUser.require();
        String accountType = user.getAccountType();
        Owner own = ownerService.mine();
        Long ownId = own.getId();
        if("ADMIN".equals(accountType)){
            return pet;
        }
        if("OWNER".equals(accountType)){
            Long currentOwnerId = pet.getOwnerId();
            if(!currentOwnerId.equals(ownId)){
                throw new AccessDeniedException("不能查询其他宠物主人的宠物");
            }
            return pet;
        }
        throw new AccessDeniedException("当前账号类型无权查询宠物");
    }

    @Override
    public Pet create(PetRequest r) {
        Pet pet = new Pet();
        pet.setOwnerId(r.ownerId());
        pet.setTypeId(r.typeId());
        pet.setName(r.name());
        //新增状态初始化为ACTIVE
        pet.setStatus("ACTIVE");
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
        //更新不存在的宠物返回 404
        if(pet==null){
            throw new BusinessException(HttpStatus.NOT_FOUND,"宠物不存在");
        }
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
        //停用不存在的宠物返回 404
        if(pet==null){
            throw new BusinessException(HttpStatus.NOT_FOUND,"宠物不存在");
        }
        pet.setStatus("INACTIVE");
        petMapper.updateById(pet);
    }
}
