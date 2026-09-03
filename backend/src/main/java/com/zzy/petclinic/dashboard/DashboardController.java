package com.zzy.petclinic.dashboard;

import com.zzy.petclinic.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "数据看板")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('dashboard:read')")
public class DashboardController {
  // 不恰当：Controller 直接依赖 Mapper，绕过了 Service 层的权限兜底。
  // private final DashboardMapper mapper;
  private final DashboardService service;

  @GetMapping("/summary")
  public ApiResponse<Map<String, Object>> summary() {
    // return ApiResponse.ok(mapper.summary());
    return ApiResponse.ok(service.summary());
  }

  @GetMapping("/visit-trend")
  public ApiResponse<List<Map<String, Object>>> trend() {
    // return ApiResponse.ok(mapper.visitTrend());
    return ApiResponse.ok(service.visitTrend());
  }

  @GetMapping("/pet-distribution")
  public ApiResponse<List<Map<String, Object>>> distribution() {
    // return ApiResponse.ok(mapper.petDistribution());
    return ApiResponse.ok(service.petDistribution());
  }

  @GetMapping("/vet-workload")
  public ApiResponse<List<Map<String, Object>>> workload() {
    // return ApiResponse.ok(mapper.vetWorkload());
    return ApiResponse.ok(service.vetWorkload());
  }

  @GetMapping("/recent-visits")
  public ApiResponse<List<Map<String, Object>>> recent() {
    // return ApiResponse.ok(mapper.recentVisits());
    return ApiResponse.ok(service.recentVisits());
  }
}
