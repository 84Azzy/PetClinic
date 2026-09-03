package com.zzy.petclinic.dashboard;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('dashboard:read')")
public class DashboardService {
  private final DashboardMapper mapper;

  public Map<String, Object> summary() {
    return mapper.summary();
  }

  public List<Map<String, Object>> visitTrend() {
    return mapper.visitTrend();
  }

  public List<Map<String, Object>> petDistribution() {
    return mapper.petDistribution();
  }

  public List<Map<String, Object>> vetWorkload() {
    return mapper.vetWorkload();
  }

  public List<Map<String, Object>> recentVisits() {
    return mapper.recentVisits();
  }
}
