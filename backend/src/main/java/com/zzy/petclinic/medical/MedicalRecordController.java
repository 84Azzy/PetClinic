package com.zzy.petclinic.medical;

import com.zzy.petclinic.common.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "电子病历")
@RestController
@RequestMapping("/api/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {
  private final MedicalRecordService s;

  @GetMapping
  public ApiResponse<PageResponse<MedicalRecord>> page(
      @Valid PageQuery q, @RequestParam(required = false) Long petId) {
    return ApiResponse.ok(s.page(q, petId));
  }

  @GetMapping("/{id}")
  public ApiResponse<MedicalRecord> get(@PathVariable Long id) {
    return ApiResponse.ok(s.get(id));
  }

  @PostMapping
  public ApiResponse<MedicalRecord> create(@Valid @RequestBody MedicalRecordRequest r) {
    return ApiResponse.created(s.create(r));
  }

  @PutMapping("/{id}")
  public ApiResponse<MedicalRecord> update(
      @PathVariable Long id, @Valid @RequestBody MedicalRecordRequest r) {
    return ApiResponse.ok(s.update(id, r));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    s.delete(id);
    return ApiResponse.message("病历已删除");
  }
}
