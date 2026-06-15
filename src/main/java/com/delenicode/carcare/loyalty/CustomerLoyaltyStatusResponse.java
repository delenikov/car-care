package com.delenicode.carcare.loyalty;

import java.math.BigDecimal;

public record CustomerLoyaltyStatusResponse(
    Long customerId,
    long completedServices,
    int requiredServices,
    boolean loyal,
    BigDecimal discountPercent) {
}
