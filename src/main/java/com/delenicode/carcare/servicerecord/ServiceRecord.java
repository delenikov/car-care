package com.delenicode.carcare.servicerecord;

import com.delenicode.carcare.common.BaseEntity;
import com.delenicode.carcare.customer.Customer;
import com.delenicode.carcare.vehicle.Vehicle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "service_records")
public class ServiceRecord extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vehicle_id", nullable = false)
  private Vehicle vehicle;

  @Column(nullable = false)
  private LocalDate serviceDate;

  @Column(nullable = false, length = 160)
  private String serviceType;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal totalAmount;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal partsCost;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal laborCost;

  private Integer odometer;

  @Column(length = 2000)
  private String replacedParts;

  @Column(length = 2000)
  private String notes;
}
