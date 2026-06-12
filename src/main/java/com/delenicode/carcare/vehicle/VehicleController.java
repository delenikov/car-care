package com.delenicode.carcare.vehicle;

import com.delenicode.carcare.common.ApiResponse;
import com.delenicode.carcare.servicerecord.ServiceRecordResponse;
import com.delenicode.carcare.servicerecord.ServiceRecordService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vehicles")
public class VehicleController {
  private final VehicleService vehicles;
  private final ServiceRecordService serviceRecords;

  @GetMapping
  ApiResponse<List<VehicleResponse>> all(@RequestParam(required = false) String vin, @RequestParam(required = false) String plateNumber, @RequestParam(required = false) String owner) {
    return ApiResponse.ok("Vehicles loaded", vehicles.search(vin, plateNumber, owner));
  }

  @GetMapping("/{id}")
  ApiResponse<VehicleResponse> get(@PathVariable Long id) {
    return ApiResponse.ok("Vehicle loaded", vehicles.findById(id));
  }

  @GetMapping("/{id}/service-history")
  ApiResponse<List<ServiceRecordResponse>> serviceHistory(@PathVariable Long id) {
    vehicles.findById(id);
    return ApiResponse.ok("Vehicle service history loaded", serviceRecords.findByVehicleId(id));
  }

  @PostMapping
  ApiResponse<VehicleResponse> create(@Valid @RequestBody VehicleRequest request) {
    return ApiResponse.ok("Vehicle created", vehicles.create(request));
  }

  @PutMapping("/{id}")
  ApiResponse<VehicleResponse> update(@PathVariable Long id, @Valid @RequestBody VehicleRequest request) {
    return ApiResponse.ok("Vehicle updated", vehicles.update(id, request));
  }
}
