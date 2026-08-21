package com.zzy.petclinic.owner;

import com.zzy.petclinic.common.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "主人档案")
@RestController
@RequestMapping("/api/owners")
@RequiredArgsConstructor
public class OwnerController {
  private final OwnerService service;

  @GetMapping
  public ApiResponse<PageResponse<Owner>> page(@Valid PageQuery query) {
    return ApiResponse.ok(service.page(query));
  }

  @GetMapping("/{id}")
  public ApiResponse<Owner> get(@PathVariable Long id) {
    return ApiResponse.ok(service.get(id));
  }

  @PostMapping
  public ApiResponse<Owner> create(@Valid @RequestBody OwnerRequest r) {
    return ApiResponse.created(service.create(r));
  }

  @PutMapping("/{id}")
  public ApiResponse<Owner> update(@PathVariable Long id, @Valid @RequestBody OwnerRequest r) {
    return ApiResponse.ok(service.update(id, r));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> disable(@PathVariable Long id) {
    service.disable(id);
    return ApiResponse.message("主人档案已停用");
  }

  @GetMapping("/me")
  public ApiResponse<Owner> mine() {
    return ApiResponse.ok(service.mine());
  }

  @PutMapping("/me")
  public ApiResponse<Owner> updateMine(@Valid @RequestBody OwnerRequest r) {
    return ApiResponse.ok(service.updateMine(r));
  }
}
