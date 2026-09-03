package com.zzy.petclinic.rbac.authorization;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 用户的角色码与权限码。
 *
 * <p>角色码不带 {@code ROLE_} 前缀，权限码保持数据库中的原始值。
 */
public record UserAuthorities(Set<String> roleCodes, Set<String> permissionCodes) {
  public UserAuthorities {
    roleCodes = immutableNonBlank(roleCodes);
    permissionCodes = immutableNonBlank(permissionCodes);
  }

  public static UserAuthorities empty() {
    return new UserAuthorities(Set.of(), Set.of());
  }

  public Collection<String> authorityCodes() {
    return Stream.concat(
            roleCodes.stream().map(role -> "ROLE_" + role), permissionCodes.stream())
        .toList();
  }

  private static Set<String> immutableNonBlank(Collection<String> values) {
    if (values == null) return Set.of();
    LinkedHashSet<String> result = new LinkedHashSet<>();
    values.stream().filter(value -> value != null && !value.isBlank()).forEach(result::add);
    return Set.copyOf(result);
  }
}
