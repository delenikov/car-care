package com.delenicode.carcare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.delenicode.carcare.customer.Customer;
import com.delenicode.carcare.customer.CustomerRepository;
import com.delenicode.carcare.document.DocumentType;
import com.delenicode.carcare.document.ServiceDocument;
import com.delenicode.carcare.document.ServiceDocumentRepository;
import com.delenicode.carcare.document.ServiceDocumentService;
import com.delenicode.carcare.notification.EmailDeliveryResult;
import com.delenicode.carcare.notification.EmailService;
import com.delenicode.carcare.notification.PdfService;
import com.delenicode.carcare.servicerecord.ServiceRecord;
import com.delenicode.carcare.servicerecord.ServiceRecordRepository;
import com.delenicode.carcare.vehicle.Vehicle;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceDocumentServiceTest {
  @Mock
  ServiceDocumentRepository documents;
  @Mock
  CustomerRepository customers;
  @Mock
  ServiceRecordRepository serviceRecords;
  @Mock
  EmailService emailService;
  @Mock
  PdfService pdfService;

  ServiceDocumentService serviceDocumentService;

  @BeforeEach
  void setUp() {
    serviceDocumentService = new ServiceDocumentService(documents, customers, serviceRecords, emailService, pdfService);
  }

  @Test
  void findAllListsLatestDocumentsFirst() {
    ServiceDocument latest = document();
    latest.setId(41L);
    latest.setCreatedAt(Instant.parse("2026-06-13T20:00:00Z"));
    latest.setFileName("latest.pdf");
    ServiceDocument older = document();
    older.setId(40L);
    older.setCreatedAt(Instant.parse("2026-06-12T20:00:00Z"));
    older.setFileName("older.pdf");
    when(documents.findAllByOrderByCreatedAtDescIdDesc()).thenReturn(List.of(latest, older));

    var response = serviceDocumentService.findAll();

    assertThat(response).extracting("fileName").containsExactly("latest.pdf", "older.pdf");
  }

  @Test
  void generateForServiceRecordCreatesPdfMetadata() {
    ServiceRecord record = serviceRecord();
    when(documents.save(any(ServiceDocument.class))).thenAnswer(invocation -> {
      ServiceDocument document = invocation.getArgument(0);
      document.setId(40L);
      return document;
    });

    var response = serviceDocumentService.generateForServiceRecord(record);

    assertThat(response.customerId()).isEqualTo(10L);
    assertThat(response.serviceRecordId()).isEqualTo(30L);
    assertThat(response.type()).isEqualTo(DocumentType.INSPECTION);
    assertThat(response.contentType()).isEqualTo("application/pdf");
  }

  @Test
  void sendEmailsGeneratedDocumentToCustomer() {
    ServiceDocument document = document();
    when(documents.findById(40L)).thenReturn(Optional.of(document));
    when(pdfService.renderServiceReport(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn("%PDF".getBytes());
    when(emailService.sendHtmlWithAttachment(any(), any(), any(), any(), any(), any(), any())).thenReturn(new EmailDeliveryResult("ada@carcare.test", "Извештај за завршен сервис: Minor Service", true, "Email sent"));

    serviceDocumentService.send(40L);

    verify(emailService).sendHtmlWithAttachment(eq("ada@carcare.test"), eq("Извештај за завршен сервис: Minor Service"), contains("225.000 km"), contains("Километража: 225000 km"), eq("service-record-30.pdf"), eq("application/pdf"), any(byte[].class));
  }

  @Test
  void exportPdfRendersDocumentPdf() {
    ServiceDocument document = document();
    when(documents.findById(40L)).thenReturn(Optional.of(document));
    when(pdfService.renderServiceReport(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn("%PDF".getBytes());

    assertThat(serviceDocumentService.exportPdf(40L)).startsWith("%PDF".getBytes());
  }

  private ServiceDocument document() {
    ServiceDocument document = new ServiceDocument();
    document.setId(40L);
    document.setCustomer(customer());
    document.setServiceRecord(serviceRecord());
    document.setType(DocumentType.INSPECTION);
    document.setFileName("service-record-30.pdf");
    document.setContentType("application/pdf");
    document.setStorageKey("generated/service-record-30.pdf");
    return document;
  }

  private ServiceRecord serviceRecord() {
    ServiceRecord record = new ServiceRecord();
    record.setId(30L);
    record.setCustomer(customer());
    record.setVehicle(vehicle());
    record.setServiceDate(LocalDate.of(2026, 6, 12));
    record.setServiceType("Minor Service");
    record.setPartsCost(new BigDecimal("1500.00"));
    record.setLaborCost(new BigDecimal("2000.00"));
    record.setTotalAmount(new BigDecimal("3500.00"));
    record.setOdometer(225000);
    record.setReplacedParts("Oil filter");
    record.setNotes("Completed");
    return record;
  }

  private Customer customer() {
    Customer customer = new Customer();
    customer.setId(10L);
    customer.setFirstName("Ada");
    customer.setLastName("Lovelace");
    customer.setFullName("Ada Lovelace");
    customer.setEmail("ada@carcare.test");
    customer.setAddress("Analytical Lane");
    return customer;
  }

  private Vehicle vehicle() {
    Vehicle vehicle = new Vehicle();
    vehicle.setId(20L);
    vehicle.setCustomer(customer());
    vehicle.setPlateNumber("SK-1234-AA");
    vehicle.setMake("Volkswagen");
    vehicle.setModel("Golf");
    vehicle.setModelYear(2020);
    return vehicle;
  }
}
