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

@Configuration
@EnableConfigurationProperties({CosStorageProperties.class, ImageStorageProperties.class})
public class CosStorageConfiguration {
  @Bean(destroyMethod = "shutdown")
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
    ClientConfig clientConfig = new ClientConfig(new Region(properties.getRegion()));
    clientConfig.setHttpProtocol(com.qcloud.cos.http.HttpProtocol.https);
    return new COSClient(credentialsProvider, clientConfig);
  }

  private void require(String value, String environmentVariable) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalStateException(environmentVariable + " must be configured when COS is enabled");
    }
  }
}
