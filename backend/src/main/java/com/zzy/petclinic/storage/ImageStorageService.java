package com.zzy.petclinic.storage;

/**
 * 图片对象存储的统一抽象。
 *
 * <p>业务层只依赖此接口，不直接依赖腾讯云 COS SDK。这样可以把“宠物/兽医图片的业务规则”与“文件实际存在哪里”分开，
 * 也便于在 COS 未启用时替换为 {@link DisabledImageStorageService}，或在测试中使用模拟实现。
 *
 * <p>这里传递和返回的是对象 Key（例如 {@code petclinic/dev/pets/7/uuid.jpg}），不是可公开访问的 URL。
 * 本项目使用私有桶，图片必须经后端鉴权后读取。
 */
public interface ImageStorageService {
  /**
   * 把已校验的图片写入对象存储。
   *
   * @param category 业务分类，用于组织对象目录，如 {@code pets} 或 {@code vets}
   * @param entityId 图片所属业务实体的主键
   * @param image 已完成格式、大小和尺寸校验的图片内容
   * @return 写入成功后的对象 Key，供数据库持久化
   */
  String store(String category, Long entityId, ValidatedImage image);

  /**
   * 根据数据库保存的对象 Key 打开图片流。
   *
   * @param objectKey COS 中的对象 Key
   * @return 包含输入流和 HTTP 元数据的图片对象；调用方必须关闭它
   */
  StoredImage load(String objectKey);

  /**
   * 删除对象存储中的图片。
   *
   * @param objectKey COS 中的对象 Key
   */
  void delete(String objectKey);
}
