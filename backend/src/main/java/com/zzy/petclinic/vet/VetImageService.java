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

/**
 * 编排兽医头像的校验、COS 存储和数据库对象 Key 更新。
 *
 * <p>流程与 {@code PetImageService} 相同：先上传新对象，再提交新 Key，最后清理旧对象。这样替换期间始终至少有
 * 一张可用图片，并为数据库更新失败提供删除新对象的补偿机会。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VetImageService {
  private final VetMapper vetMapper;
  private final VetService vetService;
  private final ImageStorageService storageService;
  private final ImageUploadValidator validator;

  /**
   * 上传并替换兽医头像。
   *
   * @param vetId 兽医主键
   * @param file 浏览器 multipart 请求中的图片
   * @return 已带新 {@code avatarUrl} 计算属性的兽医对象
   */
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

  /** 根据数据库中的对象 Key 从私有 COS 打开头像流。 */
  @PreAuthorize("isAuthenticated()")
  public StoredImage load(Long vetId) {
    Vet vet = vetService.get(vetId);
    if (!StringUtils.hasText(vet.getAvatarObjectKey())) {
      throw BusinessException.notFound("兽医头像");
    }
    return storageService.load(vet.getAvatarObjectKey());
  }

  /** 先清空数据库对象 Key，再尽力删除 COS 里的旧头像。 */
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

  /** 删除失败只记录日志，避免把已经成功的数据库更新报告为失败。 */
  private void safelyDelete(String objectKey) {
    if (!StringUtils.hasText(objectKey)) return;
    try {
      storageService.delete(objectKey);
    } catch (RuntimeException e) {
      log.warn("Failed to delete COS object {}; it can be cleaned up later", objectKey, e);
    }
  }
}
