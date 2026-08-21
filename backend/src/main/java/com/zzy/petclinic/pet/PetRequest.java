package com.zzy.petclinic.pet;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record PetRequest(
    @NotNull Long ownerId,
    @NotNull Long typeId,
    @NotBlank @Size(max = 50) String name,
    String gender,
    String breed,
    @PastOrPresent LocalDate birthDate,
    String color,
    String microchipNo,
    String allergies,
    String photoUrl) {}
