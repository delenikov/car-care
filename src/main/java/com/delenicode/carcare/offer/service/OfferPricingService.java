package com.delenicode.carcare.offer.service;


import com.delenicode.carcare.offer.dto.request.OfferPartRequest;
import com.delenicode.carcare.offer.dto.request.OfferRequest;
import com.delenicode.carcare.offer.exception.InvalidOfferException;
import com.delenicode.carcare.offer.model.Offer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OfferPricingService {
  private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

  public List<OfferPartRequest> requestedParts(OfferRequest request) {
    return request.parts() == null ? List.of() : request.parts();
  }

  public void validateParts(List<OfferPartRequest> requestedParts) {
    for (OfferPartRequest requestedPart : requestedParts) {
      if (requestedPart.name() == null || requestedPart.name().isBlank()) {
        throw new InvalidOfferException("Part name is required");
      }
      rejectNegative(requestedPart.price(), "Part price must not be negative");
    }
  }

  public BigDecimal partsCost(List<OfferPartRequest> requestedParts, BigDecimal fallbackPartsCost) {
    BigDecimal value = requestedParts.isEmpty()
        ? costOrZero(fallbackPartsCost)
        : requestedParts.stream().map(OfferPartRequest::price).reduce(BigDecimal.ZERO, BigDecimal::add);
    rejectNegative(value, "Parts cost must not be negative");
    return money(value);
  }

  public BigDecimal laborCost(BigDecimal laborCost, BigDecimal legacyAmountFallback) {
    BigDecimal value = laborCost != null ? laborCost : costOrZero(legacyAmountFallback);
    rejectNegative(value, "Labor cost must not be negative");
    return money(value);
  }

  public BigDecimal discountAmount(BigDecimal subtotalAmount, BigDecimal discountPercent) {
    if (discountPercent == null || discountPercent.signum() <= 0) {
      return money(BigDecimal.ZERO);
    }
    return money(subtotalAmount.multiply(discountPercent).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP));
  }

  public BigDecimal subtotalAmount(Offer offer) {
    return offer.getSubtotalAmount() == null ? money(offer.getPartsCost().add(offer.getLaborCost())) : money(offer.getSubtotalAmount());
  }

  public BigDecimal discountPercent(Offer offer) {
    return offer.getDiscountPercent() == null ? money(BigDecimal.ZERO) : money(offer.getDiscountPercent());
  }

  public BigDecimal discountAmount(Offer offer) {
    return offer.getDiscountAmount() == null ? discountAmount(subtotalAmount(offer), discountPercent(offer)) : money(offer.getDiscountAmount());
  }

  public BigDecimal amount(Offer offer) {
    return offer.getAmount() == null ? money(subtotalAmount(offer).subtract(discountAmount(offer))) : money(offer.getAmount());
  }

  public BigDecimal money(BigDecimal value) {
    return costOrZero(value).setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal costOrZero(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private void rejectNegative(BigDecimal value, String message) {
    if (value == null || value.signum() < 0) {
      throw new InvalidOfferException(message);
    }
  }
}
