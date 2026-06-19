package com.delenicode.carcare.servicerecord.dto.request;


import com.delenicode.carcare.servicerecord.model.ServiceRecord;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ServiceRecordRequest(
    @NotNull Long customerId,
    @NotNull Long vehicleId,
    @NotNull LocalDate serviceDate,
    @NotBlank String serviceType,
    BigDecimal partsCost,
    BigDecimal laborCost,
    BigDecimal totalAmount,
    Integer odometer,
    String replacedParts,
    String notes) {
}
