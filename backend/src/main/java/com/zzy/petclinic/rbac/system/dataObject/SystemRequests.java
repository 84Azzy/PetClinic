package com.zzy.petclinic.rbac.system.dataObject;

import jakarta.validation.constraints.*;
import java.util.List;

/** system 管理接口的请求 DTO 集合；这里只做格式校验，唯一性、状态流转和关联完整性仍由 Service 校验。 */
public final class SystemRequests {
  private SystemRequests() {}

  /** 用户新增/编辑参数；password 只在新增时作为初始密码使用，普通编辑不应据此覆盖已有密码。 */
  public record UserSave(
      @NotBlank String username,
      @NotBlank String displayName,
      String phone,
      @Email String email,
      @NotBlank String accountType,
      String password) {}

  /** 角色新增/编辑参数；status 为空时由 Service 使用 ACTIVE。 */
  public record RoleSave(
      @NotBlank String code, @NotBlank String name, String description, String status) {}

  /** 权限节点新增/编辑参数；type 的允许值为 MENU、BUTTON、API。 */
  public record PermissionSave(
      Long parentId,
      @NotBlank String code,
      @NotBlank String name,
      @NotBlank String type,
      String path,
      String icon,
      Integer sortOrder,
      String status) {}

  /** 关联关系的完整目标 id 集合；Service 需要去重、拒绝 null 元素并校验每个 id 存在。 */
  public record Ids(@NotNull List<Long> ids) {}

  /** 管理员重置密码参数；Service 仍需哈希密码并递增 tokenVersion。 */
  public record PasswordReset(@NotBlank @Size(min = 6) String password) {}

  /** 账号状态变更参数；Service 只接受约定的状态枚举值。 */
  public record StatusChange(@NotBlank String status) {}
}
