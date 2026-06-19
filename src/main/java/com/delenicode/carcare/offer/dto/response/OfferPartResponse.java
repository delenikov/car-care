package com.delenicode.carcare.offer.dto.response;


import com.delenicode.carcare.offer.model.Offer;
import java.math.BigDecimal;

public record OfferPartResponse(String name, BigDecimal price) {
}
