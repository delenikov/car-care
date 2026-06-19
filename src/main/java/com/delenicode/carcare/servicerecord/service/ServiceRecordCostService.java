package com.delenicode.carcare.servicerecord.service;


import com.delenicode.carcare.servicerecord.dto.request.ServiceRecordRequest;
import com.delenicode.carcare.servicerecord.exception.InvalidServiceRecordException;
import com.delenicode.carcare.servicerecord.model.ServiceRecord;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class ServiceRecordCostService {
  public BigDecimal partsCost(ServiceRecordRequest request) {
    BigDecimal value = costOrZero(request.partsCost());
    rejectNegative(value, "Parts cost must not be negative");
    return money(value);
  }

  public BigDecimal laborCost(ServiceRecordRequest request) {
    BigDecimal value = request.laborCost() == null ? costOrZero(request.totalAmount()) : request.laborCost();
    rejectNegative(value, "Labor cost must not be negative");
    return money(value);
  }

  public BigDecimal totalAmount(BigDecimal partsCost, BigDecimal laborCost) {
    return money(partsCost.add(laborCost));
  }

  public BigDecimal money(BigDecimal value) {
    return costOrZero(value).setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal costOrZero(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private void rejectNegative(BigDecimal value, String message) {
    if (value.signum() < 0) {
      throw new InvalidServiceRecordException(message);
    }
  }
}
