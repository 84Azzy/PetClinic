package com.zzy.petclinic.contracts;

import static org.junit.jupiter.api.Assertions.*;

import com.zzy.petclinic.ai.AiContracts;
import com.zzy.petclinic.owner.OwnerRequest;
import com.zzy.petclinic.visit.VisitRequest;
import jakarta.validation.*;
import org.junit.jupiter.api.*;

class ValidationContractTest {
  private Validator validator;

  @BeforeEach
  void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void ownerPhoneMustBeMobile() {
    assertFalse(validator.validate(new OwnerRequest(1L, "张三", "123", "bad", null, null)).isEmpty());
  }

  @Test
  void visitNeedsReason() {
    assertFalse(validator.validate(new VisitRequest(1L, 1L, "id", "")).isEmpty());
  }

  @Test
  void aiMessageCannotBeBlank() {
    assertFalse(validator.validate(new AiContracts.ChatRequest(null, " ")).isEmpty());
  }

  @Test
  void validVisitPasses() {
    assertTrue(validator.validate(new VisitRequest(1L, 2L, "request-id", "常规检查")).isEmpty());
  }
}
