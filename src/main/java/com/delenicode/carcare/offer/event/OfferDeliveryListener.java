package com.delenicode.carcare.offer.event;


import com.delenicode.carcare.offer.model.Offer;
import com.delenicode.carcare.offer.service.OfferDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OfferDeliveryListener {
  private final OfferDeliveryService deliveryService;

  @TransactionalEventListener(classes = OfferCreatedEvent.class, phase = TransactionPhase.AFTER_COMMIT)
  public void deliverAfterCommit(OfferCreatedEvent event) {
    log.info("Delivering offer {} after offer-created event", event.offerId());
    try {
      deliveryService.deliver(event.offerId());
    } catch (RuntimeException ex) {
      log.warn("Offer delivery failed after commit for offer {}", event.offerId(), ex);
    }
  }
}
