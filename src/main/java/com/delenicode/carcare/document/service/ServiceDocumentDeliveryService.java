package com.delenicode.carcare.document.service;


import com.delenicode.carcare.document.dto.response.ServiceDocumentResponse;
import com.delenicode.carcare.document.exception.DocumentNotFoundException;
import com.delenicode.carcare.document.exception.ServiceDocumentDeliveryException;
import com.delenicode.carcare.document.mapper.ServiceDocumentMapper;
import com.delenicode.carcare.document.model.ServiceDocument;
import com.delenicode.carcare.document.model.ServiceDocumentEmail;
import com.delenicode.carcare.document.repository.ServiceDocumentRepository;
import com.delenicode.carcare.notification.EmailDeliveryResult;
import com.delenicode.carcare.notification.HtmlAttachmentEmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceDocumentDeliveryService {
  private final ServiceDocumentRepository documents;
  private final ServiceDocumentEmailRenderer renderer;
  private final ServiceDocumentPdfExporter pdfExporter;
  private final ServiceDocumentMapper mapper;
  private final HtmlAttachmentEmailSender emailSender;

  public ServiceDocumentResponse deliver(Long id) {
    ServiceDocument document = documents.findByIdWithDetails(id).orElseThrow(() -> new DocumentNotFoundException(id));
    log.info("Service document delivery started. Document ID: {}. Customer ID: {}. Service record ID: {}", document.getId(), document.getCustomer().getId(), document.getServiceRecord() == null ? null : document.getServiceRecord().getId());
    ServiceDocumentEmail email = renderer.render(document);
    EmailDeliveryResult result = emailSender.sendHtmlWithAttachment(
        document.getCustomer().getEmail(),
        email.subject(),
        email.htmlBody(),
        email.textBody(),
        document.getFileName(),
        document.getContentType(),
        pdfExporter.export(document));
    if (!result.accepted()) {
      log.warn("Service document delivery failed. Document ID: {}. Customer ID: {}. Message: {}", document.getId(), document.getCustomer().getId(), result.message());
      throw new ServiceDocumentDeliveryException(result.message());
    }
    log.info("Service document delivery succeeded. Document ID: {}. Customer ID: {}", document.getId(), document.getCustomer().getId());
    return mapper.toResponse(document);
  }
}
