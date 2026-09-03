package com.zzy.petclinic.audit;

import com.zzy.petclinic.common.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "操作日志")
@RestController
@RequestMapping("/api/operation-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('system:manage')")
public class OperationLogController {
  // 不恰当：Controller 直接拼查询并调用 Mapper，Service 层无法提供二次授权保护。
  // private final OperationLogMapper mapper;
  private final OperationLogService service;

  @GetMapping
  public ApiResponse<PageResponse<OperationLog>> page(@Valid PageQuery q) {
    // return ApiResponse.ok(PageResponse.of(mapper.selectPage(...)));
    return ApiResponse.ok(service.page(q));
  }
}
