package com.zzy.petclinic.vaccination;

import com.zzy.petclinic.common.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "疫苗记录")
@RestController
@RequestMapping("/api/vaccinations")
@RequiredArgsConstructor
public class VaccinationController {
  private final VaccinationService s;

  @GetMapping
  public ApiResponse<PageResponse<VaccinationRecord>> page(
      @Valid PageQuery q,
      @RequestParam(required = false) Long petId,
      @RequestParam(required = false) Boolean overdue) {
    return ApiResponse.ok(s.page(q, petId, overdue));
  }

  @GetMapping("/{id}")
  public ApiResponse<VaccinationRecord> get(@PathVariable Long id) {
    return ApiResponse.ok(s.get(id));
  }

  @PostMapping
  public ApiResponse<VaccinationRecord> create(@Valid @RequestBody VaccinationRequest r) {
    return ApiResponse.created(s.create(r));
  }

  @PutMapping("/{id}")
  public ApiResponse<VaccinationRecord> update(
      @PathVariable Long id, @Valid @RequestBody VaccinationRequest r) {
    return ApiResponse.ok(s.update(id, r));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    s.delete(id);
    return ApiResponse.message("疫苗记录已删除");
  }
}
