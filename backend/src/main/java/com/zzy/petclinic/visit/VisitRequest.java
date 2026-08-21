package com.zzy.petclinic.visit;

import jakarta.validation.constraints.*;

public record VisitRequest(
    @NotNull Long petId,
    @NotNull Long slotId,
    @NotBlank @Size(max = 64) String requestId,
    @NotBlank @Size(max = 500) String reason) {}
