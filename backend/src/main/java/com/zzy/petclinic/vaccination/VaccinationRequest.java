package com.zzy.petclinic.vaccination;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record VaccinationRequest(
    @NotNull Long petId,
    @NotBlank String vaccineName,
    String batchNo,
    @NotNull @PastOrPresent LocalDate vaccinatedDate,
    LocalDate nextDueDate,
    String veterinarian,
    String notes) {}
