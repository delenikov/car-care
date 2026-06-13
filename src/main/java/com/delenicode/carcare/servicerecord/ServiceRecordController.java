package com.delenicode.carcare.servicerecord;

import com.delenicode.carcare.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/service-records")
public class ServiceRecordController {
  private final ServiceRecordService serviceRecords;

  @GetMapping
  ApiResponse<List<ServiceRecordResponse>> all() {
    return ApiResponse.ok("Service records loaded", serviceRecords.findAll());
  }

  @GetMapping("/{id}")
  ApiResponse<ServiceRecordResponse> one(@PathVariable Long id) {
    return ApiResponse.ok("Service record loaded", serviceRecords.findById(id));
  }

  @PostMapping
  ApiResponse<ServiceRecordResponse> create(@Valid @RequestBody ServiceRecordRequest request) {
    return ApiResponse.ok("Service record created", serviceRecords.create(request));
  }
}
