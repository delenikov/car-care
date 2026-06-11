package com.delenicode.carcare.document;

public record ServiceDocumentResponse(Long id, Long customerId, Long serviceRecordId, DocumentType type, String fileName, String contentType, String storageKey) {
}
