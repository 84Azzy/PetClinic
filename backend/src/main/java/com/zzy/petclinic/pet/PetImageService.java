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

/**
 * 编排宠物照片的业务流程：校验文件、操作 COS、更新数据库以及清理旧对象。
 *
 * <p>这里是数据库与对象存储之间的一致性边界。二者无法参加同一个本地事务，因此替换时采用“先上传新图，再更新
 * 数据库，最后删除旧图”的顺序：数据库更新失败会补偿删除新图；旧图删除失败只记录日志，不能让已经成功的业务
 * 操作对用户显示失败。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PetImageService {
  private final PetMapper petMapper;
  private final PetService petService;
  private final ImageStorageService imageStorageService;
  private final ImageUploadValidator validator;

  /**
   * 上传新照片并替换宠物当前的对象 Key。
   *
   * @param petId 宠物主键，也是 COS Key 路径的一部分
   * @param file 浏览器 multipart 请求中的图片
   * @return 已带新 {@code photoUrl} 计算属性的宠物对象
   */
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

  /**
   * 按宠物编号查到数据库中的对象 Key，再从私有 COS 打开图片流。
   *
   * <p>权限校验位于 Service，避免该方法从 Controller 之外被调用时绕过资源访问规则。
   */
  @PreAuthorize("hasAuthority('pet:manage')")
  public StoredImage load(Long petId) {
    Pet pet = petService.get(petId);
    if (!StringUtils.hasText(pet.getPhotoObjectKey())) {
      throw BusinessException.notFound("宠物照片");
    }
    return imageStorageService.load(pet.getPhotoObjectKey());
  }

  /** 先清空数据库对象 Key，再尽力删除 COS 里的旧对象。 */
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

  /**
   * 尽力清理不再被数据库引用的对象。
   *
   * <p>COS 临时故障不应回滚已成功的数据库修改；失败对象可根据日志稍后清理。
   */
  private void safelyDelete(String objectKey) {
    if (!StringUtils.hasText(objectKey)) return;
    try {
      imageStorageService.delete(objectKey);
    } catch (RuntimeException e) {
      log.warn("Failed to delete COS object {}; it can be cleaned up later", objectKey, e);
    }
  }
}
