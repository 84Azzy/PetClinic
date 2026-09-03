package com.zzy.petclinic.schedule;

import com.zzy.petclinic.common.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "排班管理")
@RestController
@RequestMapping("/api/slots")
@RequiredArgsConstructor
public class ScheduleController {
  private final ScheduleService s;

  @GetMapping
  @PreAuthorize("hasAuthority('schedule:manage')")
  public ApiResponse<PageResponse<VetScheduleSlot>> page(
      @Valid PageQuery q,
      @RequestParam(required = false) Long vetId,
      @RequestParam(required = false) LocalDate date,
      @RequestParam(required = false) String status) {
    return ApiResponse.ok(s.page(q, vetId, date, status));
  }

  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<VetScheduleSlot> get(@PathVariable Long id) {
    return ApiResponse.ok(s.get(id));
  }

  @GetMapping("/available")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<List<VetScheduleSlot>> available(
      @RequestParam Long vetId, @RequestParam(required = false) LocalDate date) {
    return ApiResponse.ok(s.available(vetId, date));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('schedule:manage') && hasAnyRole('ADMIN', 'STAFF')")
  public ApiResponse<VetScheduleSlot> create(@Valid @RequestBody SlotRequest r) {
    return ApiResponse.created(s.create(r));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('schedule:manage') && hasAnyRole('ADMIN', 'STAFF')")
  public ApiResponse<VetScheduleSlot> update(
      @PathVariable Long id, @Valid @RequestBody SlotRequest r) {
    return ApiResponse.ok(s.update(id, r));
  }

  @PostMapping("/vets/{vetId}/batch")
  @PreAuthorize("hasAuthority('schedule:manage') && hasAnyRole('ADMIN', 'STAFF')")
  public ApiResponse<Integer> batch(
      @PathVariable Long vetId, @Valid @RequestBody BatchSlotRequest r) {
    return ApiResponse.ok(s.batch(vetId, r));
  }

  @PostMapping("/{id}/close")
  @PreAuthorize("hasAuthority('schedule:manage') && hasAnyRole('ADMIN', 'STAFF')")
  public ApiResponse<Void> close(@PathVariable Long id) {
    s.close(id);
    return ApiResponse.message("时段已关闭");
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('schedule:manage') && hasAnyRole('ADMIN', 'STAFF')")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    s.delete(id);
    return ApiResponse.message("时段已删除");
  }
}
