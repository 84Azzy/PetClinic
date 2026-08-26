package com.zzy.petclinic.contracts;

import static org.junit.jupiter.api.Assertions.*;

import com.zzy.petclinic.ai.*;
import com.zzy.petclinic.common.FeatureNotImplementedException;
import com.zzy.petclinic.pet.*;
import com.zzy.petclinic.system.*;
import com.zzy.petclinic.visit.*;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReservedContractTest {
  @Test
  void petCreateIs501() {
    var e =
        assertThrows(
            FeatureNotImplementedException.class,
            () ->
                new PetController()
                    .create(
                        new PetRequest(
                            1L,
                            1L,
                            "糯米",
                            "FEMALE",
                            "英短",
                            LocalDate.now().minusYears(1),
                            null,
                            null,
                            null,
                            null)));
    assertEquals(501, e.getStatus().value());
  }

  @Test
  void petListIs501() {
    assertThrows(FeatureNotImplementedException.class, () -> new PetController().mine());
  }

  @Test
  void visitCreateIs501() {
    assertThrows(
        FeatureNotImplementedException.class,
        () -> new VisitController().create(new VisitRequest(1L, 1L, "request-1", "检查")));
  }

  @Test
  void visitCancelIs501() {
    assertThrows(
        FeatureNotImplementedException.class,
        () -> new VisitController().cancel(1L, new CancelVisitRequest("行程有变")));
  }

  @Test
  void userAdminIs501() {
    assertThrows(FeatureNotImplementedException.class, () -> new UserAdminController().page(null));
  }

  @Test
  void roleAdminIs501() {
    assertThrows(FeatureNotImplementedException.class, () -> new RoleController().list());
  }

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
