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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PetServiceImpl implements PetService{

    private final PetMapper petMapper;
    private final CurrentUser currentUser;
    private final OwnerService ownerService;

    @Override
    @PreAuthorize("hasAuthority('pet:manage')")
    public PageResponse<Pet> page(PageQuery query, Long ownerId) {
        /*
         * 不恰当：原实现直接使用调用者传入的 ownerId，没有在 Service 层限制 OWNER 的查询范围。
         * if (ownerId != null) {
         *     wrapper.eq(Pet::getOwnerId, ownerId);
         * }
         */
        AuthenticatedUser user = currentUser.require();
        Long effectiveOwnerId = ownerId;
        if (hasRole(user, "OWNER")) {
            Long currentOwnerId = ownerService.mine().getId();
            if (ownerId != null && !ownerId.equals(currentOwnerId)) {
                throw new AccessDeniedException("不能查询其他宠物主人的宠物");
            }
            effectiveOwnerId = currentOwnerId;
        } else if (!hasRole(user, "ADMIN") && !hasRole(user, "STAFF")) {
            throw new AccessDeniedException("当前角色无权查询宠物");
        }

        Page<Pet> page = new Page<>(query.pageValue(), query.sizeValue());
        LambdaQueryWrapper<Pet> wrapper = new LambdaQueryWrapper<>();
        if(effectiveOwnerId!=null){
            wrapper.eq(Pet::getOwnerId,effectiveOwnerId);
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
    @PreAuthorize("hasRole('OWNER')")
    public List<Pet> mine() {
        // 不恰当：Service 信任外部传入的 userId。
        // return petMapper.selectMine(userId);
        return petMapper.selectMine(currentUser.id());
    }

    @Override
    @PreAuthorize("hasAuthority('pet:manage')")
    public Pet get(Long id) {
        Pet pet = petMapper.selectById(id);
        //查询不存在的宠物返回 404
        if(pet==null){
            throw new BusinessException(HttpStatus.NOT_FOUND,"宠物不存在");
        }
        AuthenticatedUser user = currentUser.require();
        /*
         * 不恰当：先查 ownerService.mine() 再判断管理员，ADMIN/STAFF 没有主人档案时会错误返回 404；
         * 同时 accountType 不是 RBAC 授权关系，角色应来自 GrantedAuthority。
         */
        if (hasRole(user, "ADMIN") || hasRole(user, "STAFF")) {
            return pet;
        }
        if (hasRole(user, "OWNER")) {
            Owner own = ownerService.mine();
            if (!pet.getOwnerId().equals(own.getId())) {
                throw new AccessDeniedException("不能查询其他宠物主人的宠物");
            }
            return pet;
        }
        throw new AccessDeniedException("当前账号类型无权查询宠物");
    }

    @Override
    @PreAuthorize("hasAuthority('pet:create') && hasAnyRole('ADMIN', 'STAFF')")
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
    @PreAuthorize("hasAuthority('pet:update') && hasAnyRole('ADMIN', 'STAFF')")
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
    @PreAuthorize("hasAuthority('pet:delete') && hasAnyRole('ADMIN', 'STAFF')")
    public void delete(Long id) {
        Pet pet = petMapper.selectById(id);
        //停用不存在的宠物返回 404
        if(pet==null){
            throw new BusinessException(HttpStatus.NOT_FOUND,"宠物不存在");
        }
        pet.setStatus("INACTIVE");
        petMapper.updateById(pet);
    }

    private boolean hasRole(AuthenticatedUser user, String role) {
        String authority = "ROLE_" + role;
        return user.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }
}
