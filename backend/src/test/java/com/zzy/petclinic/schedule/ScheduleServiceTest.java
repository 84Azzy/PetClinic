package com.zzy.petclinic.schedule;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.zzy.petclinic.common.BusinessException;
import com.zzy.petclinic.vet.*;
import java.time.*;
import org.junit.jupiter.api.*;

class ScheduleServiceTest {
  private VetScheduleSlotMapper mapper;
  private ScheduleService service;

  @BeforeEach
  void setUp() {
    mapper = mock(VetScheduleSlotMapper.class);
    service = new ScheduleService(mapper, mock(VetService.class));
  }

  @Test
  void createRejectsReversedTime() {
    var start = LocalDateTime.now();
    assertEquals(
        400,
        assertThrows(
                BusinessException.class,
                () -> service.create(new SlotRequest(1L, start, start.minusMinutes(1), null)))
            .getStatus()
            .value());
  }

  @Test
  void createRejectsDuplicateStart() {
    when(mapper.selectCount(any())).thenReturn(1L);
    var start = LocalDateTime.now();
    assertEquals(
        409,
        assertThrows(
                BusinessException.class,
                () -> service.create(new SlotRequest(1L, start, start.plusMinutes(30), null)))
            .getStatus()
            .value());
  }

  @Test
  void createBuildsAvailableSlot() {
    when(mapper.selectCount(any())).thenReturn(0L);
    var start = LocalDateTime.now();
    var slot = service.create(new SlotRequest(1L, start, start.plusMinutes(30), "门诊"));
    assertEquals("AVAILABLE", slot.getStatus());
    verify(mapper).insert(slot);
  }

  @Test
  void bookedSlotCannotClose() {
    var slot = new VetScheduleSlot();
    slot.setId(1L);
    slot.setStatus("BOOKED");
    when(mapper.selectById(1L)).thenReturn(slot);
    assertThrows(BusinessException.class, () -> service.close(1L));
  }

  @Test
  void availableSlotCanClose() {
    var slot = new VetScheduleSlot();
    slot.setId(1L);
    slot.setStatus("AVAILABLE");
    when(mapper.selectById(1L)).thenReturn(slot);
    service.close(1L);
    assertEquals("CLOSED", slot.getStatus());
    verify(mapper).updateById(slot);
  }

  @Test
  void missingSlotIs404() {
    assertEquals(
        404, assertThrows(BusinessException.class, () -> service.get(99L)).getStatus().value());
  }
}
