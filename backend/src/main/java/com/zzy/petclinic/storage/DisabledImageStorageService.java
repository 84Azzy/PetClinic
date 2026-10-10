package com.zzy.petclinic.storage;

import com.zzy.petclinic.common.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * COS 关闭时注入的占位实现。
 *
 * <p>它让应用在未配置 COS 的本地环境仍可启动，但任何图片读写都会返回 503，而不是在启动时因为缺少
 * {@link ImageStorageService} Bean 而失败。启用 COS 后，本实现会被 {@link CosImageStorageService} 替代。
 */
@Service
//只有配置里`app.storage.cos.enabled=false` 时，Spring 才会执行这个方法并把`DisabledImageStorageService` 注册进容器
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
