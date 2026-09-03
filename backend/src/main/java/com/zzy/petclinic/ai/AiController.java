package com.zzy.petclinic.ai;

import com.zzy.petclinic.common.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI 助手（学习者实现）")
@RestController
@RequestMapping("/api/ai")
@PreAuthorize("hasAuthority('ai:chat')")
public class AiController {
  @PostMapping("/chat")
  public ApiResponse<AiContracts.ChatResponse> chat(@Valid @RequestBody AiContracts.ChatRequest r) {
    throw todo();
  }

  @GetMapping("/conversations")
  public ApiResponse<List<AiConversation>> conversations() {
    throw todo();
  }

  @GetMapping("/conversations/{id}/messages")
  public ApiResponse<List<AiMessage>> messages(@PathVariable Long id) {
    throw todo();
  }

  @DeleteMapping("/conversations/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    throw todo();
  }

  private FeatureNotImplementedException todo() {
    return new FeatureNotImplementedException("AI 对话、Tool Calling 与预约草稿");
  }
}
