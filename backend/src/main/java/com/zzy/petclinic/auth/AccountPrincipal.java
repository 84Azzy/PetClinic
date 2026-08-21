package com.zzy.petclinic.auth;

public record AccountPrincipal(
    Long id, String username, String displayName, String accountType, int tokenVersion) {}
