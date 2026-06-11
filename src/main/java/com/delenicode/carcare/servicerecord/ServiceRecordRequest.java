package com.delenicode.carcare.servicerecord;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ServiceRecordRequest(@NotNull Long customerId, @NotNull Long vehicleId, @NotNull LocalDate serviceDate, @NotBlank String serviceType, @NotNull BigDecimal totalAmount, Integer odometer, String notes) {
}
