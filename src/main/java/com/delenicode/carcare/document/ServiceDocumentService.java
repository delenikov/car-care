package com.delenicode.carcare.document;

import com.delenicode.carcare.customer.CustomerRepository;
import com.delenicode.carcare.servicerecord.ServiceRecord;
import com.delenicode.carcare.servicerecord.ServiceRecordRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServiceDocumentService {
  private final ServiceDocumentRepository documents;
  private final CustomerRepository customers;
  private final ServiceRecordRepository serviceRecords;

  @Transactional(readOnly = true)
  public List<ServiceDocumentResponse> findAll() {
    return documents.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional
  public ServiceDocumentResponse create(ServiceDocumentRequest request) {
    ServiceDocument document = new ServiceDocument();
    document.setCustomer(customers.findById(request.customerId()).orElseThrow(() -> new IllegalArgumentException("Customer not found")));
    if (request.serviceRecordId() != null) {
      document.setServiceRecord(serviceRecords.findById(request.serviceRecordId()).orElseThrow(() -> new IllegalArgumentException("Service record not found")));
    }
    document.setType(request.type() == null ? DocumentType.OTHER : request.type());
    document.setFileName(request.fileName());
    document.setContentType(request.contentType());
    document.setStorageKey(request.storageKey());
    return toResponse(documents.save(document));
  }

  public ServiceDocumentResponse toResponse(ServiceDocument document) {
    ServiceRecord record = document.getServiceRecord();
    return new ServiceDocumentResponse(document.getId(), document.getCustomer().getId(), record == null ? null : record.getId(), document.getType(), document.getFileName(), document.getContentType(), document.getStorageKey());
  }
}
