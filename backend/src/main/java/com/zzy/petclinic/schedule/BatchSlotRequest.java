package com.zzy.petclinic.schedule;

import jakarta.validation.constraints.*;
import java.time.*;

public record BatchSlotRequest(
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @NotNull LocalTime dailyStart,
    @NotNull LocalTime dailyEnd,
    @Min(15) @Max(240) int intervalMinutes,
    String note) {}
