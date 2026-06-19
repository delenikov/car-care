package com.delenicode.carcare;


import com.delenicode.carcare.servicerecord.model.ServiceRecord;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.delenicode.carcare.loyalty.service.CustomerLoyaltyService;
import com.delenicode.carcare.servicerecord.repository.ServiceRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerLoyaltyServiceTest {
  @Mock
  ServiceRecordRepository serviceRecords;

  CustomerLoyaltyService loyaltyService;

  @BeforeEach
  void setUp() {
    loyaltyService = new CustomerLoyaltyService(serviceRecords);
  }

  @Test
  void identifiesLoyalCustomerAfterFiveCompletedServices() {
    when(serviceRecords.countByCustomerIdAndCustomerDeletedFalse(10L)).thenReturn(5L);

    var status = loyaltyService.statusForCustomer(10L);

    assertThat(status.loyal()).isTrue();
    assertThat(status.completedServices()).isEqualTo(5);
    assertThat(status.requiredServices()).isEqualTo(5);
    assertThat(status.discountPercent()).isEqualByComparingTo("10.00");
  }

  @Test
  void doesNotDiscountCustomerBelowFiveCompletedServices() {
    when(serviceRecords.countByCustomerIdAndCustomerDeletedFalse(10L)).thenReturn(4L);

    var status = loyaltyService.statusForCustomer(10L);

    assertThat(status.loyal()).isFalse();
    assertThat(status.discountPercent()).isEqualByComparingTo("0.00");
  }
}
