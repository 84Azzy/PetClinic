package com.zzy.petclinic.visit;

import com.zzy.petclinic.common.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@Tag(name = "预约就诊（学习者实现）")
@RestController
@RequestMapping("/api/visits")
public class VisitController {
  @Operation(summary = "创建预约：需实现幂等、归属校验和时段条件抢占")
  @PostMapping
  public ApiResponse<Visit> create(@Valid @RequestBody VisitRequest r) {
    throw todo();
  }

  @GetMapping
  public ApiResponse<PageResponse<Visit>> page(
      @Valid PageQuery q,
      @RequestParam(required = false) Long petId,
      @RequestParam(required = false) Long vetId) {
    throw todo();
  }

  @GetMapping("/mine")
  public ApiResponse<List<Visit>> mine() {
    throw todo();
  }

  @GetMapping("/{id}")
  public ApiResponse<Visit> get(@PathVariable Long id) {
    throw todo();
  }

  @Operation(summary = "取消预约并在同一事务释放时段")
  @PostMapping("/{id}/cancel")
  public ApiResponse<Visit> cancel(
      @PathVariable Long id, @Valid @RequestBody CancelVisitRequest r) {
    throw todo();
  }

  @PostMapping("/{id}/complete")
  public ApiResponse<Visit> complete(@PathVariable Long id) {
    throw todo();
  }

  private FeatureNotImplementedException todo() {
    return new FeatureNotImplementedException("Visit 预约事务纵向链路");
  }
}
