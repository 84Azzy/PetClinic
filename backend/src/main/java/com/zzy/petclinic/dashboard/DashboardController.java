package com.zzy.petclinic.dashboard;

import com.zzy.petclinic.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "数据看板")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
  private final DashboardMapper mapper;

  @GetMapping("/summary")
  public ApiResponse<Map<String, Object>> summary() {
    return ApiResponse.ok(mapper.summary());
  }

  @GetMapping("/visit-trend")
  public ApiResponse<List<Map<String, Object>>> trend() {
    return ApiResponse.ok(mapper.visitTrend());
  }

  @GetMapping("/pet-distribution")
  public ApiResponse<List<Map<String, Object>>> distribution() {
    return ApiResponse.ok(mapper.petDistribution());
  }

  @GetMapping("/vet-workload")
  public ApiResponse<List<Map<String, Object>>> workload() {
    return ApiResponse.ok(mapper.vetWorkload());
  }

  @GetMapping("/recent-visits")
  public ApiResponse<List<Map<String, Object>>> recent() {
    return ApiResponse.ok(mapper.recentVisits());
  }
}
