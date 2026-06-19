package com.delenicode.carcare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.delenicode.carcare.customer.Customer;
import com.delenicode.carcare.notification.EmailDeliveryResult;
import com.delenicode.carcare.notification.HtmlEmailSender;
import com.delenicode.carcare.offer.Offer;
import com.delenicode.carcare.offer.OfferDeliveryException;
import com.delenicode.carcare.offer.OfferDeliveryService;
import com.delenicode.carcare.offer.OfferEmail;
import com.delenicode.carcare.offer.OfferEmailRenderer;
import com.delenicode.carcare.offer.OfferMapper;
import com.delenicode.carcare.offer.OfferPricingService;
import com.delenicode.carcare.offer.OfferRepository;
import com.delenicode.carcare.offer.OfferResponse;
import com.delenicode.carcare.offer.OfferStatus;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

@ExtendWith(MockitoExtension.class)
class OfferDeliveryServiceTest {
  @Mock
  OfferRepository offers;
  @Mock
  OfferEmailRenderer renderer;
  @Mock
  HtmlEmailSender emailSender;

  OfferDeliveryService deliveryService;

  @BeforeEach
  void setUp() {
    OfferPricingService pricing = new OfferPricingService();
    OfferMapper mapper = new OfferMapper(pricing);
    deliveryService = new OfferDeliveryService(offers, renderer, emailSender, mapper, new ImmediateTransactionManager());
  }

  @Test
  void deliverSendsPendingOfferAndMarksItSent() {
    Offer offer = offer(OfferStatus.PENDING_DELIVERY);
    when(offers.findByIdWithDetails(20L)).thenReturn(Optional.of(offer));
    when(offers.save(any(Offer.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(renderer.render(offer)).thenReturn(new OfferEmail("Понуда за сервис: Brake inspection", "<strong>ПОНУДА</strong>", "Вкупно: 2.000,00 ден."));
    when(emailSender.sendHtml(any(), any(), any(), any())).thenReturn(new EmailDeliveryResult("ada@carcare.test", "Понуда", true, "Email sent"));

    OfferResponse response = deliveryService.deliver(20L);

    assertThat(response.status()).isEqualTo(OfferStatus.SENT);
    assertThat(offer.getStatus()).isEqualTo(OfferStatus.SENT);
    verify(emailSender).sendHtml(eq("ada@carcare.test"), eq("Понуда за сервис: Brake inspection"), eq("<strong>ПОНУДА</strong>"), eq("Вкупно: 2.000,00 ден."));
  }

  @Test
  void deliverDoesNotSendAlreadySentOfferAgain() {
    Offer offer = offer(OfferStatus.SENT);
    when(offers.findByIdWithDetails(20L)).thenReturn(Optional.of(offer));

    OfferResponse response = deliveryService.deliver(20L);

    assertThat(response.status()).isEqualTo(OfferStatus.SENT);
    verify(emailSender, never()).sendHtml(any(), any(), any(), any());
  }

  @Test
  void deliverMarksOfferFailedWhenMailIsRejected() {
    Offer offer = offer(OfferStatus.PENDING_DELIVERY);
    when(offers.findByIdWithDetails(20L)).thenReturn(Optional.of(offer));
    when(offers.save(any(Offer.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(renderer.render(offer)).thenReturn(new OfferEmail("Понуда", "<strong>ПОНУДА</strong>", "Понуда"));
    when(emailSender.sendHtml(any(), any(), any(), any())).thenReturn(new EmailDeliveryResult("ada@carcare.test", "Понуда", false, "SMTP unavailable"));

    assertThatThrownBy(() -> deliveryService.deliver(20L))
        .isInstanceOf(OfferDeliveryException.class)
        .hasMessageContaining("SMTP unavailable");
    assertThat(offer.getStatus()).isEqualTo(OfferStatus.DELIVERY_FAILED);
  }

  @Test
  void deliverMarksOfferFailedWhenEmailRenderingFails() {
    Offer offer = offer(OfferStatus.PENDING_DELIVERY);
    when(offers.findByIdWithDetails(20L)).thenReturn(Optional.of(offer));
    when(offers.save(any(Offer.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(renderer.render(offer)).thenThrow(new IllegalStateException("Template error"));

    assertThatThrownBy(() -> deliveryService.deliver(20L))
        .isInstanceOf(OfferDeliveryException.class)
        .hasMessageContaining("Offer delivery failed");
    assertThat(offer.getStatus()).isEqualTo(OfferStatus.DELIVERY_FAILED);
  }

  private Offer offer(OfferStatus status) {
    Customer customer = new Customer();
    customer.setId(10L);
    customer.setFullName("Ada Lovelace");
    customer.setEmail("ada@carcare.test");

    Offer offer = new Offer();
    offer.setId(20L);
    offer.setCustomer(customer);
    offer.setTitle("Brake inspection");
    offer.setDescription("Inspect brakes");
    offer.setPartsCost(new BigDecimal("1200.00"));
    offer.setLaborCost(new BigDecimal("800.00"));
    offer.setSubtotalAmount(new BigDecimal("2000.00"));
    offer.setDiscountPercent(BigDecimal.ZERO.setScale(2));
    offer.setDiscountAmount(BigDecimal.ZERO.setScale(2));
    offer.setAmount(new BigDecimal("2000.00"));
    offer.setStatus(status);
    return offer;
  }

  private static class ImmediateTransactionManager extends AbstractPlatformTransactionManager {
    @Override
    protected Object doGetTransaction() throws TransactionException {
      return new Object();
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
    }
  }
}
