package com.delenicode.carcare;


import com.delenicode.carcare.offer.dto.request.OfferPartRequest;
import com.delenicode.carcare.offer.dto.request.OfferRequest;
import com.delenicode.carcare.offer.service.OfferEmailRenderer;
import com.delenicode.carcare.vehicle.repository.VehicleRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.delenicode.carcare.customer.model.Customer;
import com.delenicode.carcare.customer.repository.CustomerRepository;
import com.delenicode.carcare.loyalty.service.CustomerLoyaltyService;
import com.delenicode.carcare.notification.PdfService;
import com.delenicode.carcare.offer.model.Offer;
import com.delenicode.carcare.offer.event.OfferCreatedEvent;
import com.delenicode.carcare.offer.service.OfferDeliveryService;
import com.delenicode.carcare.offer.mapper.OfferMapper;
import com.delenicode.carcare.offer.model.OfferPart;
import com.delenicode.carcare.offer.service.OfferPricingService;
import com.delenicode.carcare.offer.repository.OfferRepository;
import com.delenicode.carcare.offer.dto.response.OfferResponse;
import com.delenicode.carcare.offer.service.OfferService;
import com.delenicode.carcare.offer.model.OfferStatus;
import com.delenicode.carcare.vehicle.model.Vehicle;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class OfferServiceTest {
  @Mock
  OfferRepository offers;
  @Mock
  CustomerRepository customers;
  @Mock
  VehicleRepository vehicles;
  @Mock
  PdfService pdfService;
  @Mock
  CustomerLoyaltyService loyalty;
  @Mock
  ApplicationEventPublisher events;
  @Mock
  OfferEmailRenderer emailRenderer;
  @Mock
  OfferDeliveryService deliveryService;

  OfferPricingService pricing;
  OfferMapper mapper;
  OfferService offerService;

  @BeforeEach
  void setUp() {
    pricing = new OfferPricingService();
    mapper = new OfferMapper(pricing);
    offerService = new OfferService(offers, customers, vehicles, pdfService, loyalty, events, pricing, mapper, emailRenderer, deliveryService);
  }

  @Test
  void findAllReturnsPaginatedOffersWithDetailsInPageOrder() {
    Offer first = offer();
    first.setId(20L);
    Offer second = offer();
    second.setId(21L);
    second.setTitle("Oil service");
    when(offers.findPageIds(PageRequest.of(0, 2))).thenReturn(new PageImpl<>(List.of(21L, 20L), PageRequest.of(0, 2), 3));
    when(offers.findAllWithDetailsByIdIn(List.of(21L, 20L))).thenReturn(List.of(first, second));

    var response = offerService.findAll(PageRequest.of(0, 2));

    assertThat(response.page()).isZero();
    assertThat(response.size()).isEqualTo(2);
    assertThat(response.totalElements()).isEqualTo(3);
    assertThat(response.content()).extracting(OfferResponse::id).containsExactly(21L, 20L);
  }

  @Test
  void createCalculatesQuoteTotalAndPublishesDeliveryEventAfterPersisting() {
    when(customers.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(customer()));
    when(offers.save(any(Offer.class))).thenAnswer(invocation -> {
      Offer offer = invocation.getArgument(0);
      offer.setId(20L);
      return offer;
    });
    when(loyalty.discountPercentForCustomer(10L)).thenReturn(BigDecimal.ZERO.setScale(2));

    OfferResponse response = offerService.create(new OfferRequest(10L, null, "Brake inspection", "Inspect brakes", List.of(new OfferPartRequest("Brake pads", new BigDecimal("1200.00"))), null, new BigDecimal("800.00"), null, null));

    assertThat(response.partsCost()).isEqualByComparingTo("1200.00");
    assertThat(response.laborCost()).isEqualByComparingTo("800.00");
    assertThat(response.subtotalAmount()).isEqualByComparingTo("2000.00");
    assertThat(response.discountPercent()).isEqualByComparingTo("0.00");
    assertThat(response.discountAmount()).isEqualByComparingTo("0.00");
    assertThat(response.amount()).isEqualByComparingTo("2000.00");
    assertThat(response.parts()).hasSize(1);
    assertThat(response.parts().get(0).name()).isEqualTo("Brake pads");
    assertThat(response.status()).isEqualTo(OfferStatus.PENDING_DELIVERY);

    ArgumentCaptor<OfferCreatedEvent> event = ArgumentCaptor.forClass(OfferCreatedEvent.class);
    verify(events).publishEvent(event.capture());
    assertThat(event.getValue().offerId()).isEqualTo(20L);
    verifyNoInteractions(deliveryService);
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

    OfferResponse response = offerService.create(new OfferRequest(10L, null, "Brake inspection", "Inspect brakes", List.of(new OfferPartRequest("Brake pads", new BigDecimal("1200.00"))), null, new BigDecimal("800.00"), null, null));

    assertThat(response.subtotalAmount()).isEqualByComparingTo("2000.00");
    assertThat(response.discountPercent()).isEqualByComparingTo("10.00");
    assertThat(response.discountAmount()).isEqualByComparingTo("200.00");
    assertThat(response.amount()).isEqualByComparingTo("1800.00");
  }

  @Test
  void sendDelegatesToDeliveryService() {
    OfferResponse sent = mapper.toResponse(sentOffer());
    when(deliveryService.deliver(20L)).thenReturn(sent);

    OfferResponse response = offerService.send(20L);

    assertThat(response.status()).isEqualTo(OfferStatus.SENT);
    verify(deliveryService).deliver(20L);
  }

  @Test
  void exportPdfUsesRendererTextAndPdfService() {
    Offer offer = offer();
    when(offers.findByIdWithDetails(20L)).thenReturn(Optional.of(offer));
    when(emailRenderer.renderText(offer)).thenReturn("Вкупно: 2.000,00 ден.");
    when(pdfService.renderServiceSummary(any(), any())).thenReturn("%PDF".getBytes());

    assertThat(offerService.exportPdf(20L)).startsWith("%PDF".getBytes());
    verify(pdfService).renderServiceSummary(eq("Понуда за сервис: Brake inspection"), eq("Вкупно: 2.000,00 ден."));
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

  private Offer sentOffer() {
    Offer offer = offer();
    offer.setStatus(OfferStatus.SENT);
    return offer;
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
    offer.setStatus(OfferStatus.PENDING_DELIVERY);
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
