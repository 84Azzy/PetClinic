package com.zzy.petclinic.storage;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@Data
@ConfigurationProperties(prefix = "app.storage.image")
public class ImageStorageProperties {
  private DataSize maxSize = DataSize.ofMegabytes(5);
  private List<String> allowedContentTypes =
      List.of("image/jpeg", "image/png", "image/webp");
}
