package com.zzy.petclinic.authentication.DTO;

public record LoginResponse(String token, String tokenType, long expiresIn, CurrentUserResponse user) {}
