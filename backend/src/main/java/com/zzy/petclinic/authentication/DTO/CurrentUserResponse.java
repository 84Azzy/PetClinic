package com.zzy.petclinic.authentication.DTO;

import com.zzy.petclinic.authentication.AuthenticatedUser;

import java.util.List;
//作为返回给前端的当前用户 DTO
public record CurrentUserResponse(
    Long id,
    String username,
    String displayName,
    String phone,
    String email,
    String accountType,
    List<String> authorities) {
  public static CurrentUserResponse from(AuthenticatedUser user) {
    return new CurrentUserResponse(
        user.getId(),
        user.getUsername(),
        user.getDisplayName(),
        user.getPhone(),
        user.getEmail(),
        user.getAccountType(),
        user.getAuthorities().stream().map(authority -> authority.getAuthority()).toList());
  }
}
