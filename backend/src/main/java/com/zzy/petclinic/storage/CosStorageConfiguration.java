package com.zzy.petclinic.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentialsProvider;
import com.qcloud.cos.auth.COSStaticCredentialsProvider;
import com.qcloud.cos.region.Region;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 创建腾讯云 COS SDK 客户端的 Spring 配置。
 *
 * <p>只有 {@code app.storage.cos.enabled=true} 时才创建客户端。开发环境从环境变量取得静态密钥，生产环境从
 * CVM 实例角色取得临时凭证；两种模式最终都通过同一个 {@link COSClient} 供存储服务调用。
 */
@Configuration
//开启CosStorageProperties, ImageStorageProperties的配置属性绑定
@EnableConfigurationProperties({CosStorageProperties.class, ImageStorageProperties.class})
public class CosStorageConfiguration {
  /**
   * 构建线程安全、可复用的 COS 客户端，并在 Spring 容器关闭时调用 {@code shutdown()}。
   *
   * <p>不要在每次上传时新建客户端。SDK 客户端维护连接池，复用既更快，也避免连接资源泄漏。
   *
   * @param properties COS 桶、地域及凭证模式配置
   * @param objectMapper 解析 CVM 元数据服务返回的临时凭证
   * @return 已强制使用 HTTPS 的 COS 客户端
   */
  @Bean(destroyMethod = "shutdown")
  //只有配置里`app.storage.cos.enabled=true` 时，Spring 才会执行这个方法并把`COSClient` 注册进容器
  @ConditionalOnProperty(prefix = "app.storage.cos", name = "enabled", havingValue = "true")
  COSClient cosClient(CosStorageProperties properties, ObjectMapper objectMapper) {
    require(properties.getRegion(), "COS_REGION");
    require(properties.getBucket(), "COS_BUCKET");
    COSCredentialsProvider credentialsProvider;
    if (properties.getCredentialMode() == CosStorageProperties.CredentialMode.CVM_ROLE) {
      require(properties.getRoleName(), "COS_CVM_ROLE_NAME");
      credentialsProvider =
          new CvmRoleCredentialsProvider(properties.getRoleName(), objectMapper);
    } else {
      require(properties.getSecretId(), "TENCENTCLOUD_SECRET_ID");
      require(properties.getSecretKey(), "TENCENTCLOUD_SECRET_KEY");
      credentialsProvider =
          new COSStaticCredentialsProvider(
              new BasicCOSCredentials(properties.getSecretId(), properties.getSecretKey()));
    }
    ClientConfig clientConfig = new ClientConfig(new Region(properties.getRegion()));//设置地域
    clientConfig.setHttpProtocol(com.qcloud.cos.http.HttpProtocol.https);//设置协议为HTTPS
    return new COSClient(credentialsProvider, clientConfig);
  }

  private void require(String value, String environmentVariable) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalStateException(environmentVariable + " must be configured when COS is enabled");
    }
  }
}
