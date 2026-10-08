package com.zzy.petclinic.vet;

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
public class VetImageService {
  private final VetMapper vetMapper;
  private final VetService vetService;
  private final ImageStorageService storageService;
  private final ImageUploadValidator validator;

  @PreAuthorize("hasAuthority('vet:manage')")
  public Vet replace(Long vetId, MultipartFile file) {
    Vet vet = requireVet(vetId);
    ValidatedImage image = validator.validate(file);
    String newKey = storageService.store("vets", vetId, image);
    String oldKey = vet.getAvatarObjectKey();
    vet.setAvatarObjectKey(newKey);
    try {
      vetMapper.updateById(vet);
    } catch (RuntimeException e) {
      safelyDelete(newKey);
      throw e;
    }
    safelyDelete(oldKey);
    return vet;
  }

  @PreAuthorize("isAuthenticated()")
  public StoredImage load(Long vetId) {
    Vet vet = vetService.get(vetId);
    if (!StringUtils.hasText(vet.getAvatarObjectKey())) {
      throw BusinessException.notFound("兽医头像");
    }
    return storageService.load(vet.getAvatarObjectKey());
  }

  @PreAuthorize("hasAuthority('vet:manage')")
  public void delete(Long vetId) {
    Vet vet = requireVet(vetId);
    String oldKey = vet.getAvatarObjectKey();
    if (!StringUtils.hasText(oldKey)) return;
    vetMapper.update(
        null,
        new LambdaUpdateWrapper<Vet>()
            .eq(Vet::getId, vetId)
            .set(Vet::getAvatarObjectKey, null));
    safelyDelete(oldKey);
  }

  private Vet requireVet(Long vetId) {
    Vet vet = vetMapper.selectById(vetId);
    if (vet == null) throw BusinessException.notFound("兽医");
    return vet;
  }

  private void safelyDelete(String objectKey) {
    if (!StringUtils.hasText(objectKey)) return;
    try {
      storageService.delete(objectKey);
    } catch (RuntimeException e) {
      log.warn("Failed to delete COS object {}; it can be cleaned up later", objectKey, e);
    }
  }
}
