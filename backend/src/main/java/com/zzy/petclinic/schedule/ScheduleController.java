package com.zzy.petclinic.schedule;

import com.zzy.petclinic.common.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "排班管理")
@RestController
@RequestMapping("/api/slots")
@RequiredArgsConstructor
public class ScheduleController {
  private final ScheduleService s;

  @GetMapping
  public ApiResponse<PageResponse<VetScheduleSlot>> page(
      @Valid PageQuery q,
      @RequestParam(required = false) Long vetId,
      @RequestParam(required = false) LocalDate date,
      @RequestParam(required = false) String status) {
    return ApiResponse.ok(s.page(q, vetId, date, status));
  }

  @GetMapping("/{id}")
  public ApiResponse<VetScheduleSlot> get(@PathVariable Long id) {
    return ApiResponse.ok(s.get(id));
  }

  @GetMapping("/available")
  public ApiResponse<List<VetScheduleSlot>> available(
      @RequestParam Long vetId, @RequestParam(required = false) LocalDate date) {
    return ApiResponse.ok(s.available(vetId, date));
  }

  @PostMapping
  public ApiResponse<VetScheduleSlot> create(@Valid @RequestBody SlotRequest r) {
    return ApiResponse.created(s.create(r));
  }

  @PutMapping("/{id}")
  public ApiResponse<VetScheduleSlot> update(
      @PathVariable Long id, @Valid @RequestBody SlotRequest r) {
    return ApiResponse.ok(s.update(id, r));
  }

  @PostMapping("/vets/{vetId}/batch")
  public ApiResponse<Integer> batch(
      @PathVariable Long vetId, @Valid @RequestBody BatchSlotRequest r) {
    return ApiResponse.ok(s.batch(vetId, r));
  }

  @PostMapping("/{id}/close")
  public ApiResponse<Void> close(@PathVariable Long id) {
    s.close(id);
    return ApiResponse.message("时段已关闭");
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    s.delete(id);
    return ApiResponse.message("时段已删除");
  }
}
