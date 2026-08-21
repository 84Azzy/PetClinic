package com.zzy.petclinic.common;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ApiResponseTest {
  @Test
  void okUses200() {
    var r = ApiResponse.ok("data");
    assertEquals(200, r.code());
    assertEquals("data", r.data());
  }

  @Test
  void createdUses201() {
    assertEquals(201, ApiResponse.created(1L).code());
  }

  @Test
  void errorHasNoData() {
    var r = ApiResponse.error(409, "冲突");
    assertEquals(409, r.code());
    assertNull(r.data());
  }

  @Test
  void pageDefaultsAreStable() {
    var q = new PageQuery(null, null, null, null);
    assertEquals(1, q.pageValue());
    assertEquals(10, q.sizeValue());
  }
}
