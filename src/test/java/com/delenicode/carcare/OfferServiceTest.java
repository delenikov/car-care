package com.delenicode.carcare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.delenicode.carcare.customer.Customer;
import com.delenicode.carcare.customer.CustomerRepository;
import com.delenicode.carcare.loyalty.CustomerLoyaltyService;
import com.delenicode.carcare.notification.EmailDeliveryResult;
import com.delenicode.carcare.notification.EmailService;
import com.delenicode.carcare.notification.PdfService;
import com.delenicode.carcare.offer.Offer;
import com.delenicode.carcare.offer.OfferPart;
import com.delenicode.carcare.offer.OfferPartRequest;
import com.delenicode.carcare.offer.OfferRepository;
import com.delenicode.carcare.offer.OfferRequest;
import com.delenicode.carcare.offer.OfferService;
import com.delenicode.carcare.offer.OfferStatus;
import com.delenicode.carcare.vehicle.Vehicle;
import com.delenicode.carcare.vehicle.VehicleRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OfferServiceTest {
  @Mock
  OfferRepository offers;
  @Mock
  CustomerRepository customers;
  @Mock
  VehicleRepository vehicles;
  @Mock
  EmailService emailService;
  @Mock
  PdfService pdfService;
  @Mock
  CustomerLoyaltyService loyalty;

  OfferService offerService;

  @BeforeEach
  void setUp() {
    offerService = new OfferService(offers, customers, vehicles, emailService, pdfService, loyalty);
  }

  @Test
  void createCalculatesQuoteTotalFromPartsAndLabor() {
    when(customers.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(customer()));
    when(offers.save(any(Offer.class))).thenAnswer(invocation -> {
      Offer offer = invocation.getArgument(0);
      offer.setId(20L);
      return offer;
    });
    when(loyalty.discountPercentForCustomer(10L)).thenReturn(BigDecimal.ZERO.setScale(2));
    when(emailService.sendHtml(any(), any(), any(), any())).thenReturn(new EmailDeliveryResult("ada@carcare.test", "Понуда за сервис: Brake inspection", true, "Email sent"));

    var response = offerService.create(new OfferRequest(10L, null, "Brake inspection", "Inspect brakes", List.of(new OfferPartRequest("Brake pads", new BigDecimal("1200.00"))), null, new BigDecimal("800.00"), null, null));

    assertThat(response.partsCost()).isEqualByComparingTo("1200.00");
    assertThat(response.laborCost()).isEqualByComparingTo("800.00");
    assertThat(response.subtotalAmount()).isEqualByComparingTo("2000.00");
    assertThat(response.discountPercent()).isEqualByComparingTo("0.00");
    assertThat(response.discountAmount()).isEqualByComparingTo("0.00");
    assertThat(response.amount()).isEqualByComparingTo("2000.00");
    assertThat(response.parts()).hasSize(1);
    assertThat(response.parts().get(0).name()).isEqualTo("Brake pads");
    assertThat(response.status()).isEqualTo(OfferStatus.SENT);
    verify(emailService).sendHtml(eq("ada@carcare.test"), eq("Понуда за сервис: Brake inspection"), contains("ПОНУДА"), contains("Цена на делови: 1.200,00 ден."));
  }

  @Test
  void createAppliesLoyalCustomerDiscountFromBackend() {
    when(customers.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(customer()));
    when(loyalty.discountPercentForCustomer(10L)).thenReturn(new BigDecimal("10.00"));
    when(offers.save(any(Offer.class))).thenAnswer(invocation -> {
      Offer offer = invocation.getArgument(0);
      offer.setId(20L);
      return offer;
    });
    when(emailService.sendHtml(any(), any(), any(), any())).thenReturn(new EmailDeliveryResult("ada@carcare.test", "Email subject", true, "Email sent"));

    var response = offerService.create(new OfferRequest(10L, null, "Brake inspection", "Inspect brakes", List.of(new OfferPartRequest("Brake pads", new BigDecimal("1200.00"))), null, new BigDecimal("800.00"), null, null));

    assertThat(response.subtotalAmount()).isEqualByComparingTo("2000.00");
    assertThat(response.discountPercent()).isEqualByComparingTo("10.00");
    assertThat(response.discountAmount()).isEqualByComparingTo("200.00");
    assertThat(response.amount()).isEqualByComparingTo("1800.00");
    verify(emailService).sendHtml(eq("ada@carcare.test"), any(), contains("Попуст за лојален клиент"), contains("Попуст за лојален клиент"));
  }

  @Test
  void sendEmailsQuoteAndMarksItSent() {
    Offer offer = offer();
    when(offers.findById(20L)).thenReturn(Optional.of(offer));
    when(offers.save(offer)).thenReturn(offer);
    when(emailService.sendHtml(any(), any(), any(), any())).thenReturn(new EmailDeliveryResult("ada@carcare.test", "Понуда за сервис: Brake inspection", true, "Email sent"));

    var response = offerService.send(20L);

    assertThat(response.status()).isEqualTo(OfferStatus.SENT);
    assertThat(response.customerName()).isEqualTo("Ada Lovelace");
    assertThat(response.vehiclePlate()).isEqualTo("SK-20");
    assertThat(response.vehicleName()).isEqualTo("Volkswagen Golf");
    verify(emailService).sendHtml(eq("ada@carcare.test"), eq("Понуда за сервис: Brake inspection"), contains("Brake pads"), contains("Вкупно: 2.000,00 ден."));
  }

  @Test
  void exportPdfUsesPdfService() {
    Offer offer = offer();
    when(offers.findById(20L)).thenReturn(Optional.of(offer));
    when(pdfService.renderServiceSummary(any(), any())).thenReturn("%PDF".getBytes());

    assertThat(offerService.exportPdf(20L)).startsWith("%PDF".getBytes());
  }

  private Customer customer() {
    Customer customer = new Customer();
    customer.setId(10L);
    customer.setFirstName("Ada");
    customer.setLastName("Lovelace");
    customer.setFullName("Ada Lovelace");
    customer.setEmail("ada@carcare.test");
    return customer;
  }

  private Offer offer() {
    Offer offer = new Offer();
    offer.setId(20L);
    offer.setCustomer(customer());
    offer.setVehicle(vehicle());
    offer.setTitle("Brake inspection");
    offer.setDescription("Inspect brakes");
    offer.setPartsCost(new BigDecimal("1200.00"));
    offer.setLaborCost(new BigDecimal("800.00"));
    offer.setSubtotalAmount(new BigDecimal("2000.00"));
    offer.setDiscountPercent(BigDecimal.ZERO.setScale(2));
    offer.setDiscountAmount(BigDecimal.ZERO.setScale(2));
    offer.setAmount(new BigDecimal("2000.00"));
    offer.setStatus(OfferStatus.DRAFT);
    OfferPart part = new OfferPart();
    part.setOffer(offer);
    part.setName("Brake pads");
    part.setPrice(new BigDecimal("1200.00"));
    part.setPosition(0);
    offer.getParts().add(part);
    return offer;
  }

  private Vehicle vehicle() {
    Vehicle vehicle = new Vehicle();
    vehicle.setId(30L);
    vehicle.setCustomer(customer());
    vehicle.setPlateNumber("SK-20");
    vehicle.setMake("Volkswagen");
    vehicle.setModel("Golf");
    return vehicle;
  }
}
