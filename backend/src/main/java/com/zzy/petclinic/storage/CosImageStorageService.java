package com.zzy.petclinic.storage;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import java.io.ByteArrayInputStream;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 使用腾讯云 COS 保存、读取和删除图片。
 *
 * <p>对象 Key 的结构为 {@code prefix/category/entityId/randomUuid.extension}。随机 UUID 让每次替换都生成新
 * Key，避免浏览器或 CDN 继续命中旧图；环境前缀则把开发与生产对象隔离开。数据库只保存这个 Key，不保存
 * SecretId、签名 URL 或 COS 输入流。
 *
 * <p>本服务只管理配置前缀以内的对象。读取和删除前调用 {@link #requireManagedKey(String)}，可避免数据库异常值
 * 导致应用越权操作同一个桶里的其他文件。
 */
@Service
@ConditionalOnProperty(prefix = "app.storage.cos", name = "enabled", havingValue = "true")
public class CosImageStorageService implements ImageStorageService {
  private final COSClient client;
  private final CosStorageProperties properties;

  public CosImageStorageService(COSClient client, CosStorageProperties properties) {
    this.client = client;
    this.properties = properties;
  }

  /**
   * 生成对象 Key、附加 HTTP 元数据并把图片字节上传到私有桶。
   *
   * <p>{@code Content-Type} 来自后端对文件头的识别；读取图片时浏览器依赖它决定如何解码。这里返回 Key 而非
   * COS URL，因为私有桶不允许浏览器匿名访问。
   */
  @Override
  public String store(String category, Long entityId, ValidatedImage image) {
    String key =
        normalizedPrefix()
            + "/"
            + category
            + "/"
            + entityId
            + "/"
            + UUID.randomUUID()
            + "."
            + image.extension();
    
    ObjectMetadata metadata = new ObjectMetadata();// 创建对象元数据
    metadata.setContentLength(image.bytes().length);//设置对象内容长度
    metadata.setContentType(image.contentType());//设置对象内容类型，之后浏览器根据类型解码
    metadata.setCacheControl("private, max-age=300");//设置缓存控制为私有，缓存300秒
    // 构建上传请求
    PutObjectRequest request =
        new PutObjectRequest(
            properties.getBucket(), key, new ByteArrayInputStream(image.bytes()), metadata);
    client.putObject(request);
    return key;
  }

  /**
   * 从 COS 打开对象流，但不在这里一次性读入内存。
   *
   * <p>返回值最终由 Controller 直接流式写给浏览器，适合图片这类二进制响应。
   */
  @Override
  public StoredImage load(String objectKey) {
    COSObject object = client.getObject(properties.getBucket(), requireManagedKey(objectKey));
    ObjectMetadata metadata = object.getObjectMetadata();
    return new StoredImage(
        object.getObjectContent(),
        metadata.getContentType(),
        metadata.getContentLength(),
        metadata.getETag());
  }

  /** 删除属于当前应用前缀的 COS 对象；空 Key 表示无需删除。 */
  @Override
  public void delete(String objectKey) {
    if (!StringUtils.hasText(objectKey)) return;
    client.deleteObject(properties.getBucket(), requireManagedKey(objectKey));
  }

  /**
   * 确认 Key 属于当前环境的应用前缀。
   *
   * <p>这是对象级的纵深防御：即使调用方或数据库传入了意外 Key，也不能借此读取/删除桶内其他路径。
   */
  private String requireManagedKey(String objectKey) {
    String prefix = normalizedPrefix() + "/";
    if (!StringUtils.hasText(objectKey) || !objectKey.startsWith(prefix)) {
      throw new IllegalArgumentException("Object key is outside the configured application prefix");
    }
    return objectKey;
  }

  /** 去掉配置前缀首尾的斜杠，避免拼出双斜杠或绝对路径风格的 Key。 */
  private String normalizedPrefix() {
    String prefix = properties.getPrefix();
    if (!StringUtils.hasText(prefix)) return "petclinic";
    return prefix.replaceAll("^/+|/+$", "");
  }
}
