package com.zzy.petclinic.vet;

import jakarta.validation.constraints.*;
import java.util.List;

public record VetRequest(
    @NotBlank @Size(max = 50) String name,
    @Pattern(regexp = "^1\\d{10}$") String phone,
    @Email String email,
    @NotBlank String licenseNo,
    String biography,
    String avatarUrl,
    String status,
    List<Long> specialtyIds) {}
