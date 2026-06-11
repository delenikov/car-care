package com.delenicode.carcare.offer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record OfferRequest(@NotNull Long customerId, Long vehicleId, @NotBlank String title, String description, @NotNull BigDecimal amount, LocalDate expiresOn) {
}
