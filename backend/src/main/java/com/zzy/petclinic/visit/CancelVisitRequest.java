package com.zzy.petclinic.visit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelVisitRequest(@NotBlank @Size(max = 255) String reason) {}
