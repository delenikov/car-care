package com.delenicode.carcare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.delenicode.carcare.customer.Customer;
import com.delenicode.carcare.document.DocumentType;
import com.delenicode.carcare.document.ServiceDocument;
import com.delenicode.carcare.document.ServiceDocumentPdfExporter;
import com.delenicode.carcare.document.ServiceDocumentViewFactory;
import com.delenicode.carcare.notification.PdfService;
import com.delenicode.carcare.servicerecord.ServiceRecord;
import com.delenicode.carcare.vehicle.Vehicle;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceDocumentPdfExporterTest {
  @Mock
  PdfService pdfService;

  @Test
  void exportRendersServiceReportPdfFromDocumentView() {
    ServiceDocument document = document();
    ServiceDocumentPdfExporter exporter = new ServiceDocumentPdfExporter(pdfService, new ServiceDocumentViewFactory());
    when(pdfService.renderServiceReport(
        eq("30"),
        eq("Ada Lovelace"),
        eq("Analytical Lane"),
        eq("ada@carcare.test"),
        eq("12.06.2026"),
        eq("SK-1234-AA - Volkswagen Golf"),
        eq("225.000"),
        eq("Minor Service"),
        eq("Oil filter"),
        eq("Completed"),
        eq("1.500,00 ден."),
        eq("2.000,00 ден."),
        eq("3.500,00 ден."))).thenReturn("%PDF".getBytes());

    assertThat(exporter.export(document)).startsWith("%PDF".getBytes());

    verify(pdfService).renderServiceReport(
        eq("30"),
        eq("Ada Lovelace"),
        eq("Analytical Lane"),
        eq("ada@carcare.test"),
        eq("12.06.2026"),
        eq("SK-1234-AA - Volkswagen Golf"),
        eq("225.000"),
        eq("Minor Service"),
        eq("Oil filter"),
        eq("Completed"),
        eq("1.500,00 ден."),
        eq("2.000,00 ден."),
        eq("3.500,00 ден."));
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
    return vehicle;
  }
}
