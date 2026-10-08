package com.zzy.petclinic.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.storage.cos")
public class CosStorageProperties {
  private boolean enabled;
  private String region;
  private String bucket;
  private String prefix = "petclinic/dev";
  private CredentialMode credentialMode = CredentialMode.STATIC;
  private String secretId;
  private String secretKey;
  private String roleName;

  public enum CredentialMode {
    STATIC,
    CVM_ROLE
  }
}
