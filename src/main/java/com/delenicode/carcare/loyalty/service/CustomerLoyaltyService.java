package com.delenicode.carcare.loyalty.service;


import com.delenicode.carcare.loyalty.dto.response.CustomerLoyaltyStatusResponse;
import com.delenicode.carcare.servicerecord.model.ServiceRecord;
import com.delenicode.carcare.servicerecord.repository.ServiceRecordRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerLoyaltyService {
  public static final int REQUIRED_COMPLETED_SERVICES = 5;
  public static final BigDecimal LOYALTY_DISCOUNT_PERCENT = new BigDecimal("10.00");
  public static final BigDecimal NO_DISCOUNT_PERCENT = BigDecimal.ZERO.setScale(2);

  private final ServiceRecordRepository serviceRecords;

  @Transactional(readOnly = true)
  public CustomerLoyaltyStatusResponse statusForCustomer(Long customerId) {
    long completedServices = serviceRecords.countByCustomerIdAndCustomerDeletedFalse(customerId);
    boolean loyal = completedServices >= REQUIRED_COMPLETED_SERVICES;
    return new CustomerLoyaltyStatusResponse(
        customerId,
        completedServices,
        REQUIRED_COMPLETED_SERVICES,
        loyal,
        loyal ? LOYALTY_DISCOUNT_PERCENT : NO_DISCOUNT_PERCENT);
  }

  @Transactional(readOnly = true)
  public BigDecimal discountPercentForCustomer(Long customerId) {
    return statusForCustomer(customerId).discountPercent();
  }
}
