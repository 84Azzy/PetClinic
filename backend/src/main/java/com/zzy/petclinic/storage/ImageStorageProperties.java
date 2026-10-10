package com.zzy.petclinic.storage;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * 图片上传规则，对应 {@code app.storage.image.*}。
 *
 * <p>这些值由后端执行最终校验；前端的文件类型和 5MB 校验只用于尽早提示用户，不能作为安全边界。
 */
@Data
@ConfigurationProperties(prefix = "app.storage.image")
public class ImageStorageProperties {
  private DataSize maxSize = DataSize.ofMegabytes(5);
  private List<String> allowedContentTypes =
      List.of("image/jpeg", "image/png", "image/webp");
}
