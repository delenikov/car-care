package com.delenicode.carcare.offer;

import com.delenicode.carcare.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

  @GetMapping("/{id}")
  ApiResponse<OfferResponse> get(@PathVariable Long id) {
    return ApiResponse.ok("Offer loaded", offers.findById(id));
  }

  @PostMapping
  ApiResponse<OfferResponse> create(@Valid @RequestBody OfferRequest request) {
    return ApiResponse.ok("Offer created", offers.create(request));
  }

  @PostMapping("/{id}/send")
  ApiResponse<OfferResponse> send(@PathVariable Long id) {
    return ApiResponse.ok("Offer sent", offers.send(id));
  }

  @GetMapping("/{id}/pdf")
  ResponseEntity<byte[]> pdf(@PathVariable Long id) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=offer-" + id + ".pdf")
        .contentType(MediaType.APPLICATION_PDF)
        .body(offers.exportPdf(id));
  }
}
