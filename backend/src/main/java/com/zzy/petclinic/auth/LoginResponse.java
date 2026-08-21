package com.zzy.petclinic.auth;

public record LoginResponse(String token, String tokenType, long expiresIn, UserProfile user) {}
