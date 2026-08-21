package com.zzy.petclinic.notice;

import com.zzy.petclinic.common.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "公告管理")
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {
  private final NoticeService s;

  @GetMapping
  public ApiResponse<PageResponse<Notice>> page(@Valid PageQuery q) {
    return ApiResponse.ok(s.page(q));
  }

  @GetMapping("/active")
  public ApiResponse<List<Notice>> active() {
    return ApiResponse.ok(s.active());
  }

  @GetMapping("/{id}")
  public ApiResponse<Notice> get(@PathVariable Long id) {
    return ApiResponse.ok(s.get(id));
  }

  @PostMapping
  public ApiResponse<Notice> create(@Valid @RequestBody NoticeRequest r) {
    return ApiResponse.created(s.create(r));
  }

  @PutMapping("/{id}")
  public ApiResponse<Notice> update(@PathVariable Long id, @Valid @RequestBody NoticeRequest r) {
    return ApiResponse.ok(s.update(id, r));
  }

  @PostMapping("/{id}/publish")
  public ApiResponse<Notice> publish(@PathVariable Long id) {
    return ApiResponse.ok(s.publish(id));
  }

  @PostMapping("/{id}/withdraw")
  public ApiResponse<Notice> withdraw(@PathVariable Long id) {
    return ApiResponse.ok(s.withdraw(id));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    s.delete(id);
    return ApiResponse.message("公告已删除");
  }
}
