package com.zzy.petclinic.storage;

import com.zzy.petclinic.common.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
    prefix = "app.storage.cos",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class DisabledImageStorageService implements ImageStorageService {
  private BusinessException unavailable() {
    return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "图片存储服务尚未配置");
  }

  @Override
  public String store(String category, Long entityId, ValidatedImage image) {
    throw unavailable();
  }

  @Override
  public StoredImage load(String objectKey) {
    throw unavailable();
  }

  @Override
  public void delete(String objectKey) {
    throw unavailable();
  }
}
