package com.delenicode.carcare.document;

import com.delenicode.carcare.servicerecord.ServiceRecord;
import org.springframework.stereotype.Component;

@Component
public class ServiceDocumentMapper {
  public ServiceDocumentResponse toResponse(ServiceDocument document) {
    ServiceRecord record = document.getServiceRecord();
    return new ServiceDocumentResponse(
        document.getId(),
        document.getCustomer().getId(),
        record == null ? null : record.getId(),
        document.getType(),
        document.getFileName(),
        document.getContentType(),
        document.getStorageKey(),
        document.getCreatedAt());
  }
}
