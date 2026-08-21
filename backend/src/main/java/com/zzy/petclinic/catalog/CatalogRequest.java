package com.zzy.petclinic.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CatalogRequest(
    @NotBlank @Size(max = 50) String name, @Size(max = 255) String description, String status) {}
