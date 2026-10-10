package com.zzy.petclinic.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 将 {@code app.storage.cos.*} 配置绑定成类型安全的 Java 对象。
 *
 * <p>本地开发使用 {@link CredentialMode#STATIC}，从环境变量读取受限 CAM 子用户密钥；生产环境使用 {@link
 * CredentialMode#CVM_ROLE}，由云服务器实例角色提供可自动轮换的临时凭证。桶名必须是包含 APPID 的完整名称，
 * 如 {@code petclinic-images-dev-1250000000}。
 */
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
    /** 使用 SecretId/SecretKey，适合本地开发，密钥不得提交到仓库。 */
    STATIC,

    /** 通过 CVM 元数据服务获得实例角色临时凭证，适合生产环境。 */
    CVM_ROLE
  }
}
