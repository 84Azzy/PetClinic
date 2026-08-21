package com.zzy.petclinic.feedback;

import jakarta.validation.constraints.*;

public record ReplyFeedbackRequest(@NotBlank @Size(max = 1000) String reply) {}
