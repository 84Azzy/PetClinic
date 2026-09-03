package com.zzy.petclinic.rbac.authentication.DTO;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
