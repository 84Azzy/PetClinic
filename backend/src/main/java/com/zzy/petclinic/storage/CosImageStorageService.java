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

@Service
@ConditionalOnProperty(prefix = "app.storage.cos", name = "enabled", havingValue = "true")
public class CosImageStorageService implements ImageStorageService {
  private final COSClient client;
  private final CosStorageProperties properties;

  public CosImageStorageService(COSClient client, CosStorageProperties properties) {
    this.client = client;
    this.properties = properties;
  }

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
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentLength(image.bytes().length);
    metadata.setContentType(image.contentType());
    metadata.setCacheControl("private, max-age=300");
    PutObjectRequest request =
        new PutObjectRequest(
            properties.getBucket(), key, new ByteArrayInputStream(image.bytes()), metadata);
    client.putObject(request);
    return key;
  }

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

  @Override
  public void delete(String objectKey) {
    if (!StringUtils.hasText(objectKey)) return;
    client.deleteObject(properties.getBucket(), requireManagedKey(objectKey));
  }

  private String requireManagedKey(String objectKey) {
    String prefix = normalizedPrefix() + "/";
    if (!StringUtils.hasText(objectKey) || !objectKey.startsWith(prefix)) {
      throw new IllegalArgumentException("Object key is outside the configured application prefix");
    }
    return objectKey;
  }

  private String normalizedPrefix() {
    String prefix = properties.getPrefix();
    if (!StringUtils.hasText(prefix)) return "petclinic";
    return prefix.replaceAll("^/+|/+$", "");
  }
}
