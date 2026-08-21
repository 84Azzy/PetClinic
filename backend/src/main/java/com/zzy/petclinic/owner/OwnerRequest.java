package com.zzy.petclinic.owner;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OwnerRequest(
    Long userId,
    @NotBlank @Size(max = 50) String name,
    @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone,
    @Email String email,
    @Size(max = 255) String address,
    String status) {}
