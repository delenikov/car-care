package com.delenicode.carcare.offer.controller;


import com.delenicode.carcare.offer.dto.request.OfferRequest;
import com.delenicode.carcare.offer.dto.response.OfferResponse;
import com.delenicode.carcare.offer.model.Offer;
import com.delenicode.carcare.offer.service.OfferService;
import com.delenicode.carcare.common.ApiResponse;
import com.delenicode.carcare.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
  ApiResponse<PageResponse<OfferResponse>> all(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ApiResponse.ok("Offers loaded", offers.findAll(pageable));
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
