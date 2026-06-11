package com.delenicode.carcare.offer;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OfferResponse(Long id, Long customerId, Long vehicleId, String title, String description, BigDecimal amount, LocalDate expiresOn, OfferStatus status) {
}
