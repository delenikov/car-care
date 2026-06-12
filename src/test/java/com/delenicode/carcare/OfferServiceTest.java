package com.delenicode.carcare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.delenicode.carcare.customer.Customer;
import com.delenicode.carcare.customer.CustomerRepository;
import com.delenicode.carcare.notification.EmailService;
import com.delenicode.carcare.notification.PdfService;
import com.delenicode.carcare.offer.Offer;
import com.delenicode.carcare.offer.OfferRepository;
import com.delenicode.carcare.offer.OfferRequest;
import com.delenicode.carcare.offer.OfferService;
import com.delenicode.carcare.offer.OfferStatus;
import com.delenicode.carcare.vehicle.VehicleRepository;
import java.math.BigDecimal;
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

  OfferService offerService;

  @BeforeEach
  void setUp() {
    offerService = new OfferService(offers, customers, vehicles, emailService, pdfService);
  }

  @Test
  void createCalculatesQuoteTotalFromPartsAndLabor() {
    when(customers.findById(10L)).thenReturn(Optional.of(customer()));
    when(offers.save(any(Offer.class))).thenAnswer(invocation -> {
      Offer offer = invocation.getArgument(0);
      offer.setId(20L);
      return offer;
    });

    var response = offerService.create(new OfferRequest(10L, null, "Brake inspection", "Inspect brakes", new BigDecimal("1200.00"), new BigDecimal("800.00"), null, null));

    assertThat(response.partsCost()).isEqualByComparingTo("1200.00");
    assertThat(response.laborCost()).isEqualByComparingTo("800.00");
    assertThat(response.amount()).isEqualByComparingTo("2000.00");
  }

  @Test
  void sendEmailsQuoteAndMarksItSent() {
    Offer offer = offer();
    when(offers.findById(20L)).thenReturn(Optional.of(offer));
    when(offers.save(offer)).thenReturn(offer);

    var response = offerService.send(20L);

    assertThat(response.status()).isEqualTo(OfferStatus.SENT);
    verify(emailService).send("ada@carcare.test", "Service quotation: Brake inspection", "Parts cost: 1200.00\nLabor cost: 800.00\nTotal cost: 2000.00\nInspect brakes");
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
    offer.setTitle("Brake inspection");
    offer.setDescription("Inspect brakes");
    offer.setPartsCost(new BigDecimal("1200.00"));
    offer.setLaborCost(new BigDecimal("800.00"));
    offer.setAmount(new BigDecimal("2000.00"));
    offer.setStatus(OfferStatus.DRAFT);
    return offer;
  }
}
