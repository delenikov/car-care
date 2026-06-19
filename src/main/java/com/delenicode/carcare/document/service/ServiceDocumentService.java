package com.delenicode.carcare.document.service;


import com.delenicode.carcare.customer.model.Customer;
import com.delenicode.carcare.document.dto.request.ServiceDocumentRequest;
import com.delenicode.carcare.document.dto.response.ServiceDocumentResponse;
import com.delenicode.carcare.document.exception.DocumentNotFoundException;
import com.delenicode.carcare.document.mapper.ServiceDocumentMapper;
import com.delenicode.carcare.document.model.DocumentType;
import com.delenicode.carcare.document.model.ServiceDocument;
import com.delenicode.carcare.document.repository.ServiceDocumentRepository;
import com.delenicode.carcare.servicerecord.exception.ServiceRecordNotFoundException;
import com.delenicode.carcare.customer.repository.CustomerRepository;
import com.delenicode.carcare.customer.exception.CustomerNotFoundException;
import com.delenicode.carcare.servicerecord.model.ServiceRecord;
import com.delenicode.carcare.servicerecord.repository.ServiceRecordRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
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
    ServiceDocument saved = documents.save(document);
    log.info("Service document created. Document ID: {}. Customer ID: {}. Service record ID: {}. Type: {}. File name: {}", saved.getId(), saved.getCustomer().getId(), saved.getServiceRecord() == null ? null : saved.getServiceRecord().getId(), saved.getType(), saved.getFileName());
    return mapper.toResponse(saved);
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
    ServiceDocument saved = documents.save(document);
    log.info("Service document generated. Document ID: {}. Service record ID: {}. Customer ID: {}. File name: {}", saved.getId(), record.getId(), record.getCustomer().getId(), saved.getFileName());
    return mapper.toResponse(saved);
  }

  public ServiceDocumentResponse send(Long id) {
    return delivery.deliver(id);
  }

  public byte[] exportPdf(Long id) {
    ServiceDocument document = documents.findByIdWithDetails(id).orElseThrow(() -> new DocumentNotFoundException(id));
    log.info("Service document PDF exported. Document ID: {}. Customer ID: {}. Service record ID: {}", document.getId(), document.getCustomer().getId(), document.getServiceRecord() == null ? null : document.getServiceRecord().getId());
    return pdfExporter.export(document);
  }
}
