package com.delenicode.carcare.offer.service;


import com.delenicode.carcare.customer.model.Customer;
import com.delenicode.carcare.offer.dto.response.OfferResponse;
import com.delenicode.carcare.offer.exception.InvalidOfferException;
import com.delenicode.carcare.offer.exception.OfferDeliveryException;
import com.delenicode.carcare.offer.exception.OfferNotFoundException;
import com.delenicode.carcare.offer.mapper.OfferMapper;
import com.delenicode.carcare.offer.model.Offer;
import com.delenicode.carcare.offer.model.OfferEmail;
import com.delenicode.carcare.offer.model.OfferStatus;
import com.delenicode.carcare.offer.repository.OfferRepository;
import com.delenicode.carcare.notification.EmailDeliveryResult;
import com.delenicode.carcare.notification.HtmlEmailSender;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfferDeliveryService {
  private final OfferRepository offers;
  private final OfferEmailRenderer renderer;
  private final HtmlEmailSender emailSender;
  private final OfferMapper mapper;
  private final PlatformTransactionManager transactionManager;
  private final Set<Long> inFlightDeliveries = ConcurrentHashMap.newKeySet();

  public OfferResponse deliver(Long offerId) {
    if (!inFlightDeliveries.add(offerId)) {
      log.info("Offer delivery skipped. Reason: already in flight. Offer ID: {}", offerId);
      return currentResponse(offerId);
    }
    try {
      Offer offer = loadOffer(offerId);
      if (offer.getStatus() == OfferStatus.SENT) {
        log.info("Offer delivery skipped. Reason: already sent. Offer ID: {}", offerId);
        return mapper.toResponse(offer);
      }
      if (offer.getStatus() != OfferStatus.PENDING_DELIVERY && offer.getStatus() != OfferStatus.DELIVERY_FAILED) {
        log.warn("Offer delivery rejected. Reason: invalid status. Offer ID: {}. Status: {}", offerId, offer.getStatus());
        throw new InvalidOfferException("Offer is not pending delivery");
      }
      try {
        log.info("Offer delivery started. Offer ID: {}. Customer ID: {}. Status: {}", offerId, offer.getCustomer().getId(), offer.getStatus());
        validateEmail(offer);
        OfferEmail email = renderer.render(offer);
        EmailDeliveryResult result = emailSender.sendHtml(offer.getCustomer().getEmail(), email.subject(), email.htmlBody(), email.textBody());
        if (!result.accepted()) {
          throw new OfferDeliveryException("Offer delivery failed: " + result.message());
        }
        Offer sent = updateStatus(offerId, OfferStatus.SENT);
        log.info("Offer delivery succeeded. Offer ID: {}. Customer ID: {}. Status: {}", sent.getId(), sent.getCustomer().getId(), sent.getStatus());
        return mapper.toResponse(sent);
      } catch (InvalidOfferException ex) {
        updateStatus(offerId, OfferStatus.DELIVERY_FAILED);
        log.warn("Offer delivery failed. Offer ID: {}. Reason: invalid offer. Message: {}", offerId, ex.getMessage());
        throw ex;
      } catch (OfferDeliveryException ex) {
        updateStatus(offerId, OfferStatus.DELIVERY_FAILED);
        log.warn("Offer delivery failed. Offer ID: {}. Reason: delivery error. Message: {}", offerId, ex.getMessage());
        throw ex;
      } catch (RuntimeException ex) {
        updateStatus(offerId, OfferStatus.DELIVERY_FAILED);
        log.warn("Offer delivery failed. Offer ID: {}. Reason: unexpected error.", offerId, ex);
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
        OfferStatus previousStatus = offer.getStatus();
        offer.setStatus(offerStatus);
        log.info("Offer status changed. Offer ID: {}. From: {}. To: {}", offerId, previousStatus, offerStatus);
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
      log.warn("Offer delivery rejected. Reason: invalid email. Offer ID: {}. Customer ID: {}", offer.getId(), offer.getCustomer().getId());
      throw new InvalidOfferException("Customer email is invalid");
    }
  }
}
