package com.zzy.petclinic.medical;

import jakarta.validation.constraints.*;

public record MedicalRecordRequest(
    @NotNull Long visitId,
    @NotBlank String symptoms,
    @NotBlank String diagnosis,
    @NotBlank String treatment,
    String prescription,
    String notes) {}
