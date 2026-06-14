package com.delenicode.carcare.loyalty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record LoyaltyRuleRequest(@NotBlank String name, @NotNull BigDecimal pointsPerCurrencyUnit, @NotNull BigDecimal discountPercent, boolean active) {
}
