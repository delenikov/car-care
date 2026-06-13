package com.delenicode.carcare.offer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OfferRequest(@NotNull Long customerId, Long vehicleId, @NotBlank String title, String description, List<@Valid OfferPartRequest> parts, BigDecimal partsCost, BigDecimal laborCost, BigDecimal amount, LocalDate expiresOn) {
}
