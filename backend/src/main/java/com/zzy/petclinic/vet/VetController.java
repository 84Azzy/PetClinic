package com.zzy.petclinic.vet;

import com.zzy.petclinic.common.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "兽医管理")
@RestController
@RequestMapping("/api/vets")
@RequiredArgsConstructor
public class VetController {
  private final VetService s;

  @GetMapping
  public ApiResponse<PageResponse<Vet>> page(
      @Valid PageQuery q, @RequestParam(required = false) Long specialtyId) {
    return ApiResponse.ok(s.page(q, specialtyId));
  }

  @GetMapping("/{id}")
  public ApiResponse<Vet> get(@PathVariable Long id) {
    return ApiResponse.ok(s.get(id));
  }

  @GetMapping("/{id}/specialties")
  public ApiResponse<List<Long>> specialties(@PathVariable Long id) {
    return ApiResponse.ok(s.specialties(id));
  }

  @PostMapping
  public ApiResponse<Vet> create(@Valid @RequestBody VetRequest r) {
    return ApiResponse.created(s.create(r));
  }

  @PutMapping("/{id}")
  public ApiResponse<Vet> update(@PathVariable Long id, @Valid @RequestBody VetRequest r) {
    return ApiResponse.ok(s.update(id, r));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> disable(@PathVariable Long id) {
    s.disable(id);
    return ApiResponse.message("兽医已停用");
  }
}
