package com.delenicode.carcare.document;

import com.delenicode.carcare.customer.CustomerRepository;
import com.delenicode.carcare.customer.CustomerNotFoundException;
import com.delenicode.carcare.servicerecord.ServiceRecord;
import com.delenicode.carcare.servicerecord.ServiceRecordNotFoundException;
import com.delenicode.carcare.servicerecord.ServiceRecordRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServiceDocumentService {
  private final ServiceDocumentRepository documents;
  private final CustomerRepository customers;
  private final ServiceRecordRepository serviceRecords;
  private final ServiceDocumentMapper mapper;
  private final ServiceDocumentPdfExporter pdfExporter;
  private final ServiceDocumentDeliveryService delivery;

  @Transactional(readOnly = true)
  public List<ServiceDocumentResponse> findAll() {
    return documents.findAllByOrderByCreatedAtDescIdDesc().stream().map(mapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public ServiceDocumentResponse findById(Long id) {
    return documents.findByIdWithDetails(id)
        .map(mapper::toResponse)
        .orElseThrow(() -> new DocumentNotFoundException(id));
  }

  @Transactional
  public ServiceDocumentResponse create(ServiceDocumentRequest request) {
    ServiceDocument document = new ServiceDocument();
    document.setCustomer(customers.findById(request.customerId())
        .orElseThrow(() -> new CustomerNotFoundException(request.customerId())));
    if (request.serviceRecordId() != null) {
      document.setServiceRecord(serviceRecords.findById(request.serviceRecordId())
          .orElseThrow(() -> new ServiceRecordNotFoundException(request.serviceRecordId())));
    }
    document.setType(request.type() == null ? DocumentType.OTHER : request.type());
    document.setFileName(request.fileName());
    document.setContentType(request.contentType());
    document.setStorageKey(request.storageKey());
    return mapper.toResponse(documents.save(document));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ServiceDocumentResponse generateForServiceRecord(Long serviceRecordId) {
    ServiceRecord record = serviceRecords.findByIdWithDetails(serviceRecordId)
        .orElseThrow(() -> new ServiceRecordNotFoundException(serviceRecordId));
    return generateForServiceRecord(record);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ServiceDocumentResponse generateForServiceRecord(ServiceRecord record) {
    ServiceDocument document = new ServiceDocument();
    document.setCustomer(record.getCustomer());
    document.setServiceRecord(record);
    document.setType(DocumentType.INSPECTION);
    document.setFileName("service-record-" + record.getId() + ".pdf");
    document.setContentType("application/pdf");
    document.setStorageKey("generated/service-record-" + record.getId() + ".pdf");
    return mapper.toResponse(documents.save(document));
  }

  public ServiceDocumentResponse send(Long id) {
    return delivery.deliver(id);
  }

  public byte[] exportPdf(Long id) {
    ServiceDocument document = documents.findByIdWithDetails(id).orElseThrow(() -> new DocumentNotFoundException(id));
    return pdfExporter.export(document);
  }
}
