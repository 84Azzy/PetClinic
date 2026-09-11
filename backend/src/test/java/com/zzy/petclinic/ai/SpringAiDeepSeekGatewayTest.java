package com.zzy.petclinic.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zzy.petclinic.common.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.ObjectProvider;

/** 验证 AI 未启用时，延迟 ChatClient Bean 的创建失败也会稳定转换为 503。 */
class SpringAiDeepSeekGatewayTest {

  @Test
  void missingBuilderReturns503() {
    ObjectProvider<ChatClient.Builder> provider = provider();
    when(provider.getIfAvailable()).thenReturn(null);
    SpringAiDeepSeekGateway gateway = new SpringAiDeepSeekGateway(provider, new ObjectMapper());

    BusinessException exception =
        assertThrows(BusinessException.class, () -> gateway.generate(List.of()));

    assertEquals(503, exception.getStatus().value());
  }

  @Test
  void lazyBuilderCreationFailureAlsoReturns503() {
    ObjectProvider<ChatClient.Builder> provider = provider();
    when(provider.getIfAvailable()).thenThrow(new BeanCreationException("chatClientBuilder"));
    SpringAiDeepSeekGateway gateway = new SpringAiDeepSeekGateway(provider, new ObjectMapper());

    BusinessException exception =
        assertThrows(BusinessException.class, () -> gateway.generate(List.of()));

    assertEquals(503, exception.getStatus().value());
  }

  @SuppressWarnings("unchecked")
  private ObjectProvider<ChatClient.Builder> provider() {
    return mock(ObjectProvider.class);
  }
}
