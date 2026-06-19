package com.delenicode.carcare.document.dto.response;


import com.delenicode.carcare.document.model.DocumentType;
import java.time.Instant;

public record ServiceDocumentResponse(Long id, Long customerId, Long serviceRecordId, DocumentType type, String fileName, String contentType, String storageKey, Instant createdAt) {
}
