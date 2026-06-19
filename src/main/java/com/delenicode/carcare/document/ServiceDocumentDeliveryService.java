package com.delenicode.carcare.document;

import com.delenicode.carcare.notification.EmailDeliveryResult;
import com.delenicode.carcare.notification.HtmlAttachmentEmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ServiceDocumentDeliveryService {
  private final ServiceDocumentRepository documents;
  private final ServiceDocumentEmailRenderer renderer;
  private final ServiceDocumentPdfExporter pdfExporter;
  private final ServiceDocumentMapper mapper;
  private final HtmlAttachmentEmailSender emailSender;

  public ServiceDocumentResponse deliver(Long id) {
    ServiceDocument document = documents.findByIdWithDetails(id).orElseThrow(() -> new DocumentNotFoundException(id));
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
      throw new ServiceDocumentDeliveryException(result.message());
    }
    return mapper.toResponse(document);
  }
}
