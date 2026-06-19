package com.delenicode.carcare.loyalty.dto.response;

import java.math.BigDecimal;

public record CustomerLoyaltyStatusResponse(
    Long customerId,
    long completedServices,
    int requiredServices,
    boolean loyal,
    BigDecimal discountPercent) {
}
