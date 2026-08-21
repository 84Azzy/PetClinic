package com.zzy.petclinic.schedule;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record SlotRequest(
    @NotNull Long vetId,
    @NotNull LocalDateTime startTime,
    @NotNull LocalDateTime endTime,
    String note) {}
