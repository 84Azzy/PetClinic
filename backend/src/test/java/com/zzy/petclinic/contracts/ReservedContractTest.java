package com.zzy.petclinic.contracts;

import static org.junit.jupiter.api.Assertions.*;

import com.zzy.petclinic.ai.*;
import com.zzy.petclinic.common.FeatureNotImplementedException;
import com.zzy.petclinic.rbac.system.controller.PermissionController;
import org.junit.jupiter.api.Test;

class ReservedContractTest {
  @Test
  void permissionTreeIs501() {
    assertThrows(FeatureNotImplementedException.class, () -> new PermissionController().tree());
  }

  @Test
  void aiChatIs501() {
    assertThrows(
        FeatureNotImplementedException.class,
        () -> new AiController().chat(new AiContracts.ChatRequest(null, "查询宠物")));
  }
}
