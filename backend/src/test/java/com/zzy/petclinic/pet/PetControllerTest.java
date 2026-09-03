package com.zzy.petclinic.pet;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zzy.petclinic.common.PageQuery;
import com.zzy.petclinic.common.PageResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PetControllerTest {
  private static final PageQuery QUERY = new PageQuery(1L, 10L, null, null);

  private PetController controller;
  private PetService petService;

  @BeforeEach
  void setUp() {
    controller = new PetController();
    petService = mock(PetService.class);
    ReflectionTestUtils.setField(controller, "petService", petService);
  }

  @Test
  void pageDelegatesScopeEnforcementToService() {
    PageResponse<Pet> expected = emptyPage();
    when(petService.page(QUERY, 2L)).thenReturn(expected);

    assertSame(expected, controller.page(QUERY, 2L).data());
    verify(petService).page(QUERY, 2L);
  }

  @Test
  void mineDoesNotPassAUserIdFromController() {
    List<Pet> expected = List.of(new Pet());
    when(petService.mine()).thenReturn(expected);

    assertSame(expected, controller.mine().data());
    verify(petService).mine();
  }

  private static PageResponse<Pet> emptyPage() {
    return new PageResponse<>(List.of(), 0, 1, 10);
  }
}
