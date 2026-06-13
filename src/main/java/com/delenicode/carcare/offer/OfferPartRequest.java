package com.delenicode.carcare.offer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record OfferPartRequest(@NotBlank String name, @NotNull @PositiveOrZero BigDecimal price) {
}
