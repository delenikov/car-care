package com.delenicode.carcare.servicerecord;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ServiceRecordResponse(
    Long id,
    Long customerId,
    String customerName,
    Long vehicleId,
    String vehiclePlate,
    String vehicleName,
    LocalDate serviceDate,
    String serviceType,
    BigDecimal partsCost,
    BigDecimal laborCost,
    BigDecimal totalAmount,
    Integer odometer,
    String replacedParts,
    String notes) {
}
