package com.delenicode.carcare.offer.dto.response;


import com.delenicode.carcare.offer.model.Offer;
import com.delenicode.carcare.offer.model.OfferStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OfferResponse(
    Long id,
    Long customerId,
    String customerName,
    Long vehicleId,
    String vehiclePlate,
    String vehicleName,
    String title,
    String description,
    List<OfferPartResponse> parts,
    BigDecimal partsCost,
    BigDecimal laborCost,
    BigDecimal subtotalAmount,
    BigDecimal discountPercent,
    BigDecimal discountAmount,
    BigDecimal amount,
    LocalDate expiresOn,
    OfferStatus status) {
}
