package com.zzy.petclinic.notice;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record NoticeRequest(
    @NotBlank @Size(max = 120) String title,
    @NotBlank String content,
    LocalDateTime expiresAt,
    Integer sortOrder) {}
