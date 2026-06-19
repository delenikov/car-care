package com.delenicode.carcare.document.controller;


import com.delenicode.carcare.document.dto.request.ServiceDocumentRequest;
import com.delenicode.carcare.document.dto.response.ServiceDocumentResponse;
import com.delenicode.carcare.document.service.ServiceDocumentService;
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
@RequestMapping("/api/documents")
public class ServiceDocumentController {
  private final ServiceDocumentService documents;

  @GetMapping
  ApiResponse<List<ServiceDocumentResponse>> all() {
    return ApiResponse.ok("Documents loaded", documents.findAll());
  }

  @GetMapping("/{id}")
  ApiResponse<ServiceDocumentResponse> get(@PathVariable Long id) {
    return ApiResponse.ok("Document loaded", documents.findById(id));
  }

  @PostMapping
  ApiResponse<ServiceDocumentResponse> create(@Valid @RequestBody ServiceDocumentRequest request) {
    return ApiResponse.ok("Document created", documents.create(request));
  }

  @PostMapping("/{id}/send")
  ApiResponse<ServiceDocumentResponse> send(@PathVariable Long id) {
    return ApiResponse.ok("Document sent", documents.send(id));
  }

  @GetMapping("/{id}/pdf")
  ResponseEntity<byte[]> pdf(@PathVariable Long id) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=document-" + id + ".pdf")
        .contentType(MediaType.APPLICATION_PDF)
        .body(documents.exportPdf(id));
  }
}
