package com.delenicode.carcare.servicerecord;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ServiceRecordResponse(
    Long id,
    Long customerId,
    Long vehicleId,
    LocalDate serviceDate,
    String serviceType,
    BigDecimal partsCost,
    BigDecimal laborCost,
    BigDecimal totalAmount,
    Integer odometer,
    String replacedParts,
    String notes) {
}
