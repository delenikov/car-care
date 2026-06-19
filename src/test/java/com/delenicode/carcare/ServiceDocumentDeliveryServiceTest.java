package com.delenicode.carcare;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.delenicode.carcare.customer.Customer;
import com.delenicode.carcare.document.DocumentType;
import com.delenicode.carcare.document.ServiceDocument;
import com.delenicode.carcare.document.ServiceDocumentDeliveryException;
import com.delenicode.carcare.document.ServiceDocumentDeliveryService;
import com.delenicode.carcare.document.ServiceDocumentEmail;
import com.delenicode.carcare.document.ServiceDocumentEmailRenderer;
import com.delenicode.carcare.document.ServiceDocumentMapper;
import com.delenicode.carcare.document.ServiceDocumentPdfExporter;
import com.delenicode.carcare.document.ServiceDocumentRepository;
import com.delenicode.carcare.notification.EmailDeliveryResult;
import com.delenicode.carcare.notification.HtmlAttachmentEmailSender;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceDocumentDeliveryServiceTest {
  @Mock
  ServiceDocumentRepository documents;
  @Mock
  ServiceDocumentEmailRenderer renderer;
  @Mock
  ServiceDocumentPdfExporter pdfExporter;
  @Mock
  HtmlAttachmentEmailSender emailSender;

  ServiceDocumentDeliveryService delivery;

  @BeforeEach
  void setUp() {
    delivery = new ServiceDocumentDeliveryService(documents, renderer, pdfExporter, new ServiceDocumentMapper(), emailSender);
  }

  @Test
  void deliverSendsRenderedEmailWithPdfAttachment() {
    ServiceDocument document = document();
    when(documents.findByIdWithDetails(40L)).thenReturn(Optional.of(document));
    when(renderer.render(document)).thenReturn(new ServiceDocumentEmail("Извештај", "<strong>HTML</strong>", "Текст"));
    when(pdfExporter.export(document)).thenReturn("%PDF".getBytes());
    when(emailSender.sendHtmlWithAttachment(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(new EmailDeliveryResult("ada@carcare.test", "Извештај", true, "Email sent"));

    delivery.deliver(40L);

    verify(emailSender).sendHtmlWithAttachment(
        eq("ada@carcare.test"),
        eq("Извештај"),
        eq("<strong>HTML</strong>"),
        eq("Текст"),
        eq("service-record-30.pdf"),
        eq("application/pdf"),
        any(byte[].class));
  }

  @Test
  void deliverThrowsWhenMailServiceRejectsMessage() {
    ServiceDocument document = document();
    when(documents.findByIdWithDetails(40L)).thenReturn(Optional.of(document));
    when(renderer.render(document)).thenReturn(new ServiceDocumentEmail("Извештај", "<strong>HTML</strong>", "Текст"));
    when(pdfExporter.export(document)).thenReturn("%PDF".getBytes());
    when(emailSender.sendHtmlWithAttachment(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(new EmailDeliveryResult("ada@carcare.test", "Извештај", false, "SMTP unavailable"));

    assertThatThrownBy(() -> delivery.deliver(40L))
        .isInstanceOf(ServiceDocumentDeliveryException.class)
        .hasMessage("SMTP unavailable");
  }

  private ServiceDocument document() {
    Customer customer = new Customer();
    customer.setId(10L);
    customer.setFullName("Ada Lovelace");
    customer.setEmail("ada@carcare.test");

    ServiceDocument document = new ServiceDocument();
    document.setId(40L);
    document.setCustomer(customer);
    document.setType(DocumentType.INSPECTION);
    document.setFileName("service-record-30.pdf");
    document.setContentType("application/pdf");
    document.setStorageKey("generated/service-record-30.pdf");
    document.setCreatedAt(Instant.parse("2026-06-13T20:00:00Z"));
    return document;
  }
}
