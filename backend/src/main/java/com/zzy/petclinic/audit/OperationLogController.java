package com.zzy.petclinic.audit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzy.petclinic.common.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "操作日志")
@RestController
@RequestMapping("/api/operation-logs")
@RequiredArgsConstructor
public class OperationLogController {
  private final OperationLogMapper mapper;

  @GetMapping
  public ApiResponse<PageResponse<OperationLog>> page(@Valid PageQuery q) {
    return ApiResponse.ok(
        PageResponse.of(
            mapper.selectPage(
                Page.of(q.pageValue(), q.sizeValue()),
                new LambdaQueryWrapper<OperationLog>().orderByDesc(OperationLog::getId))));
  }
}
