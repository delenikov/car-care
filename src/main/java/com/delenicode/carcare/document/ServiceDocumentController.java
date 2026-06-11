package com.delenicode.carcare.document;

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
@RequestMapping("/api/documents")
public class ServiceDocumentController {
  private final ServiceDocumentService documents;

  @GetMapping
  ApiResponse<List<ServiceDocumentResponse>> all() {
    return ApiResponse.ok("Documents loaded", documents.findAll());
  }

  @PostMapping
  ApiResponse<ServiceDocumentResponse> create(@Valid @RequestBody ServiceDocumentRequest request) {
    return ApiResponse.ok("Document created", documents.create(request));
  }
}
