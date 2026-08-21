package com.zzy.petclinic.auth;

public record UserProfile(
    Long id, String username, String displayName, String phone, String email, String accountType) {}
