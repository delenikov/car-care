package com.delenicode.carcare.offer;

import com.delenicode.carcare.notification.EmailDeliveryResult;
import com.delenicode.carcare.notification.HtmlEmailSender;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class OfferDeliveryService {
  private final OfferRepository offers;
  private final OfferEmailRenderer renderer;
  private final HtmlEmailSender emailSender;
  private final OfferMapper mapper;
  private final PlatformTransactionManager transactionManager;
  private final Set<Long> inFlightDeliveries = ConcurrentHashMap.newKeySet();

  public OfferResponse deliver(Long offerId) {
    if (!inFlightDeliveries.add(offerId)) {
      return currentResponse(offerId);
    }
    try {
      Offer offer = loadOffer(offerId);
      if (offer.getStatus() == OfferStatus.SENT) {
        return mapper.toResponse(offer);
      }
      if (offer.getStatus() != OfferStatus.PENDING_DELIVERY && offer.getStatus() != OfferStatus.DELIVERY_FAILED) {
        throw new InvalidOfferException("Offer is not pending delivery");
      }
      try {
        validateEmail(offer);
        OfferEmail email = renderer.render(offer);
        EmailDeliveryResult result = emailSender.sendHtml(offer.getCustomer().getEmail(), email.subject(), email.htmlBody(), email.textBody());
        if (!result.accepted()) {
          throw new OfferDeliveryException("Offer delivery failed: " + result.message());
        }
        return mapper.toResponse(updateStatus(offerId, OfferStatus.SENT));
      } catch (InvalidOfferException ex) {
        updateStatus(offerId, OfferStatus.DELIVERY_FAILED);
        throw ex;
      } catch (OfferDeliveryException ex) {
        updateStatus(offerId, OfferStatus.DELIVERY_FAILED);
        throw ex;
      } catch (RuntimeException ex) {
        updateStatus(offerId, OfferStatus.DELIVERY_FAILED);
        throw new OfferDeliveryException("Offer delivery failed", ex);
      }
    } finally {
      inFlightDeliveries.remove(offerId);
    }
  }

  private OfferResponse currentResponse(Long offerId) {
    return mapper.toResponse(loadOffer(offerId));
  }

  private Offer loadOffer(Long offerId) {
    return inDeliveryTransaction(status -> offers.findByIdWithDetails(offerId).orElseThrow(() -> new OfferNotFoundException(offerId)));
  }

  private Offer updateStatus(Long offerId, OfferStatus offerStatus) {
    return inDeliveryTransaction(status -> {
      Offer offer = offers.findByIdWithDetails(offerId).orElseThrow(() -> new OfferNotFoundException(offerId));
      if (offer.getStatus() != OfferStatus.SENT || offerStatus == OfferStatus.SENT) {
        offer.setStatus(offerStatus);
      }
      return offers.save(offer);
    });
  }

  private <T> T inDeliveryTransaction(TransactionCallback<T> callback) {
    TransactionTemplate template = new TransactionTemplate(transactionManager);
    template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return template.execute(callback);
  }

  private void validateEmail(Offer offer) {
    try {
      new InternetAddress(offer.getCustomer().getEmail()).validate();
    } catch (AddressException ex) {
      updateStatus(offer.getId(), OfferStatus.DELIVERY_FAILED);
      throw new InvalidOfferException("Customer email is invalid");
    }
  }
}
