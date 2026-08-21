package com.zzy.petclinic.rbac;

import jakarta.validation.constraints.*;
import java.util.List;

public final class RbacRequests {
  private RbacRequests() {}

  public record UserSave(
      @NotBlank String username,
      @NotBlank String displayName,
      String phone,
      @Email String email,
      @NotBlank String accountType,
      String password) {}

  public record RoleSave(
      @NotBlank String code, @NotBlank String name, String description, String status) {}

  public record PermissionSave(
      Long parentId,
      @NotBlank String code,
      @NotBlank String name,
      @NotBlank String type,
      String path,
      String icon,
      Integer sortOrder,
      String status) {}

  public record Ids(@NotNull List<Long> ids) {}

  public record PasswordReset(@NotBlank @Size(min = 6) String password) {}

  public record StatusChange(@NotBlank String status) {}
}
