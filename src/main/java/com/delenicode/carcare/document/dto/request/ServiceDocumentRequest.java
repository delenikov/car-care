package com.delenicode.carcare.document.dto.request;


import com.delenicode.carcare.document.model.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ServiceDocumentRequest(@NotNull Long customerId, Long serviceRecordId, DocumentType type, @NotBlank String fileName, @NotBlank String contentType, @NotBlank String storageKey) {
}
