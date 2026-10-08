package com.zzy.petclinic.pet;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zzy.petclinic.common.BusinessException;
import com.zzy.petclinic.storage.ImageStorageService;
import com.zzy.petclinic.storage.ImageUploadValidator;
import com.zzy.petclinic.storage.StoredImage;
import com.zzy.petclinic.storage.ValidatedImage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class PetImageService {
  private final PetMapper petMapper;
  private final PetService petService;
  private final ImageStorageService imageStorageService;
  private final ImageUploadValidator validator;

  @PreAuthorize("hasAuthority('pet:update') && hasAnyRole('ADMIN', 'STAFF')")
  public Pet replace(Long petId, MultipartFile file) {
    Pet pet = requirePet(petId);
    ValidatedImage image = validator.validate(file);
    String newKey = imageStorageService.store("pets", petId, image);
    String oldKey = pet.getPhotoObjectKey();
    pet.setPhotoObjectKey(newKey);
    try {
      petMapper.updateById(pet);
    } catch (RuntimeException e) {
      safelyDelete(newKey);
      throw e;
    }
    safelyDelete(oldKey);
    return pet;
  }

  @PreAuthorize("hasAuthority('pet:manage')")
  public StoredImage load(Long petId) {
    Pet pet = petService.get(petId);
    if (!StringUtils.hasText(pet.getPhotoObjectKey())) {
      throw BusinessException.notFound("宠物照片");
    }
    return imageStorageService.load(pet.getPhotoObjectKey());
  }

  @PreAuthorize("hasAuthority('pet:update') && hasAnyRole('ADMIN', 'STAFF')")
  public void delete(Long petId) {
    Pet pet = requirePet(petId);
    String oldKey = pet.getPhotoObjectKey();
    if (!StringUtils.hasText(oldKey)) return;
    petMapper.update(
        null,
        new LambdaUpdateWrapper<Pet>()
            .eq(Pet::getId, petId)
            .set(Pet::getPhotoObjectKey, null));
    safelyDelete(oldKey);
  }

  private Pet requirePet(Long petId) {
    Pet pet = petMapper.selectById(petId);
    if (pet == null) throw BusinessException.notFound("宠物");
    return pet;
  }

  private void safelyDelete(String objectKey) {
    if (!StringUtils.hasText(objectKey)) return;
    try {
      imageStorageService.delete(objectKey);
    } catch (RuntimeException e) {
      log.warn("Failed to delete COS object {}; it can be cleaned up later", objectKey, e);
    }
  }
}
