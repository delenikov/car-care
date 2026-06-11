package com.delenicode.carcare.loyalty;

import java.math.BigDecimal;

public record LoyaltyRuleResponse(Long id, String name, BigDecimal pointsPerCurrencyUnit, BigDecimal discountPercent, boolean active) {
}
