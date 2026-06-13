package com.delenicode.carcare.offer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OfferResponse(Long id, Long customerId, Long vehicleId, String title, String description, List<OfferPartResponse> parts, BigDecimal partsCost, BigDecimal laborCost, BigDecimal amount, LocalDate expiresOn, OfferStatus status) {
}
