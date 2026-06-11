package com.delenicode.carcare.offer;

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
@RequestMapping("/api/offers")
public class OfferController {
  private final OfferService offers;

  @GetMapping
  ApiResponse<List<OfferResponse>> all() {
    return ApiResponse.ok("Offers loaded", offers.findAll());
  }

  @PostMapping
  ApiResponse<OfferResponse> create(@Valid @RequestBody OfferRequest request) {
    return ApiResponse.ok("Offer created", offers.create(request));
  }
}
