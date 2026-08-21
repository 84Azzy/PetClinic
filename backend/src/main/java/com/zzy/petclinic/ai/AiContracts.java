package com.zzy.petclinic.ai;

import jakarta.validation.constraints.*;
import java.util.List;

public final class AiContracts {
  private AiContracts() {}

  public record ChatRequest(Long conversationId, @NotBlank @Size(max = 2000) String message) {}

  public record AppointmentDraft(
      @NotNull Long petId, @NotNull Long slotId, @NotBlank String reason, String summary) {}

  public record ChatResponse(
      Long conversationId, String answer, AppointmentDraft draft, List<String> toolsUsed) {}
}
