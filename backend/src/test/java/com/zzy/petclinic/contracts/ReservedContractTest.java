package com.zzy.petclinic.contracts;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.zzy.petclinic.ai.*;
import org.junit.jupiter.api.Test;

class ReservedContractTest {
  @Test
  void aiChatDelegatesToService() {
    /*
     * Day2 的预留契约：
     * assertThrows(
     *     FeatureNotImplementedException.class,
     *     () -> new AiController().chat(new AiContracts.ChatRequest(null, "查询宠物")));
     *
     * 现在 Controller 已接线，契约应改为校验它把请求交给 Service 并包装成功响应。
     */
    AiAssistantService service = mock(AiAssistantService.class);
    AiContracts.ChatRequest request = new AiContracts.ChatRequest(null, "查询宠物");
    AiContracts.ChatResponse expected =
        new AiContracts.ChatResponse(1L, "查询完成", null, java.util.List.of("listMyPets"));
    when(service.chat(request)).thenReturn(expected);

    var response = new AiController(service).chat(request);

    assertEquals(200, response.code());
    assertSame(expected, response.data());
    verify(service).chat(request);
  }
}
