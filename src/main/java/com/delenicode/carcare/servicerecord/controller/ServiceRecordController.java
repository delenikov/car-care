package com.delenicode.carcare.servicerecord.controller;


import com.delenicode.carcare.servicerecord.dto.request.ServiceRecordRequest;
import com.delenicode.carcare.servicerecord.dto.response.ServiceRecordResponse;
import com.delenicode.carcare.servicerecord.model.ServiceRecord;
import com.delenicode.carcare.servicerecord.service.ServiceRecordService;
import com.delenicode.carcare.common.ApiResponse;
import com.delenicode.carcare.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
  ApiResponse<PageResponse<ServiceRecordResponse>> all(@PageableDefault(size = 20, sort = "serviceDate", direction = Sort.Direction.DESC) Pageable pageable) {
    return ApiResponse.ok("Service records loaded", serviceRecords.findAll(pageable));
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
