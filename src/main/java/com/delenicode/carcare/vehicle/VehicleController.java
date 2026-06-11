package com.delenicode.carcare.vehicle;

import com.delenicode.carcare.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vehicles")
public class VehicleController {
  private final VehicleService vehicles;

  @GetMapping
  ApiResponse<List<VehicleResponse>> all() {
    return ApiResponse.ok("Vehicles loaded", vehicles.findAll());
  }

  @PostMapping
  ApiResponse<VehicleResponse> create(@Valid @RequestBody VehicleRequest request) {
    return ApiResponse.ok("Vehicle created", vehicles.create(request));
  }
}
