package com.delenicode.carcare.offer.dto.request;


import com.delenicode.carcare.offer.model.Offer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record OfferPartRequest(@NotBlank String name, @NotNull @PositiveOrZero BigDecimal price) {
}
