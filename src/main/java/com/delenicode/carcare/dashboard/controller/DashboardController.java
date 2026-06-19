package com.delenicode.carcare.dashboard.controller;


import com.delenicode.carcare.dashboard.dto.response.DashboardSummary;
import com.delenicode.carcare.dashboard.service.DashboardService;
import com.delenicode.carcare.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
public class DashboardController {
  private final DashboardService dashboard;

  @GetMapping("/summary")
  ApiResponse<DashboardSummary> summary() {
    return ApiResponse.ok("Dashboard summary loaded", dashboard.summary());
  }
}
