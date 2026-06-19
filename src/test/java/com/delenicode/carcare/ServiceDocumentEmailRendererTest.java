package com.delenicode.carcare;

import static org.assertj.core.api.Assertions.assertThat;

import com.delenicode.carcare.customer.Customer;
import com.delenicode.carcare.document.DocumentType;
import com.delenicode.carcare.document.ServiceDocument;
import com.delenicode.carcare.document.ServiceDocumentEmail;
import com.delenicode.carcare.document.ServiceDocumentEmailRenderer;
import com.delenicode.carcare.document.ServiceDocumentViewFactory;
import com.delenicode.carcare.servicerecord.ServiceRecord;
import com.delenicode.carcare.vehicle.Vehicle;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class ServiceDocumentEmailRendererTest {
  @Test
  void rendersMacedonianHtmlAndPlainTextServiceDocumentEmail() {
    ServiceDocumentEmailRenderer renderer = new ServiceDocumentEmailRenderer(templateEngine(), new ServiceDocumentViewFactory());

    ServiceDocumentEmail email = renderer.render(document());

    assertThat(email.subject()).isEqualTo("Извештај за завршен сервис: Minor Service");
    assertThat(email.htmlBody()).contains("СЕРВИСЕН ИЗВЕШТАЈ", "225.000", "km", "3.500,00 ден.");
    assertThat(email.textBody()).contains("Километража: 225.000 km", "Вкупно: 3.500,00 ден.");
  }

  private TemplateEngine templateEngine() {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    return engine;
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
