package com.zzy.petclinic.feedback;

import jakarta.validation.constraints.*;

public record FeedbackRequest(
    @NotBlank String category,
    @NotBlank @Size(max = 120) String title,
    @NotBlank @Size(max = 1000) String content,
    String contact) {}
