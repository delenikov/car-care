package com.delenicode.carcare.document;

import com.delenicode.carcare.customer.CustomerRepository;
import com.delenicode.carcare.notification.EmailService;
import com.delenicode.carcare.notification.PdfService;
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
  private final EmailService emailService;
  private final PdfService pdfService;

  @Transactional(readOnly = true)
  public List<ServiceDocumentResponse> findAll() {
    return documents.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public ServiceDocumentResponse findById(Long id) {
    return documents.findById(id).map(this::toResponse).orElseThrow(() -> new IllegalArgumentException("Document not found"));
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

  @Transactional
  public ServiceDocumentResponse generateForServiceRecord(ServiceRecord record) {
    ServiceDocument document = new ServiceDocument();
    document.setCustomer(record.getCustomer());
    document.setServiceRecord(record);
    document.setType(DocumentType.INSPECTION);
    document.setFileName("service-record-" + record.getId() + ".pdf");
    document.setContentType("application/pdf");
    document.setStorageKey("generated/service-record-" + record.getId() + ".pdf");
    return toResponse(documents.save(document));
  }

  @Transactional
  public ServiceDocumentResponse send(Long id) {
    ServiceDocument document = documents.findById(id).orElseThrow(() -> new IllegalArgumentException("Document not found"));
    emailService.send(document.getCustomer().getEmail(), "Service document: " + document.getFileName(), "Your service document is attached: " + document.getFileName());
    return toResponse(document);
  }

  @Transactional(readOnly = true)
  public byte[] exportPdf(Long id) {
    ServiceDocument document = documents.findById(id).orElseThrow(() -> new IllegalArgumentException("Document not found"));
    return pdfService.renderServiceSummary(document.getFileName(), documentBody(document));
  }

  public ServiceDocumentResponse toResponse(ServiceDocument document) {
    ServiceRecord record = document.getServiceRecord();
    return new ServiceDocumentResponse(document.getId(), document.getCustomer().getId(), record == null ? null : record.getId(), document.getType(), document.getFileName(), document.getContentType(), document.getStorageKey());
  }

  private String documentBody(ServiceDocument document) {
    ServiceRecord record = document.getServiceRecord();
    if (record == null) {
      return "Document for " + document.getCustomer().getFullName();
    }
    return "Customer: " + document.getCustomer().getFullName() + "\nVehicle: " + record.getVehicle().getPlateNumber() + "\nService: " + record.getServiceType() + "\nTotal: " + record.getTotalAmount();
  }
}
