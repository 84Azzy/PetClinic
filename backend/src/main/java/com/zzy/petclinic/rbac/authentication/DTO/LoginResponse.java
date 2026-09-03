package com.zzy.petclinic.rbac.authentication.DTO;

public record LoginResponse(String token, String tokenType, long expiresIn, CurrentUserResponse user) {}
