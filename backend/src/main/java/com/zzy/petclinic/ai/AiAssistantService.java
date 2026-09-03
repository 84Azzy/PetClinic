package com.zzy.petclinic.ai;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('ai:chat')")
public interface AiAssistantService {
  AiContracts.ChatResponse chat(AiContracts.ChatRequest request);

  List<AiConversation> conversations();

  List<AiMessage> messages(Long conversationId);

  void delete(Long conversationId);
}
