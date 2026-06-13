package com.delenicode.carcare.vehicle;

import com.delenicode.carcare.common.BaseEntity;
import com.delenicode.carcare.customer.Customer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "vehicles")
public class Vehicle extends BaseEntity {
  @Column(nullable = false, unique = true, length = 40)
  private String plateNumber;

  @Column(nullable = false, length = 80)
  private String make;

  @Column(nullable = false, length = 80)
  private String model;

  private Integer modelYear;

  @Column(length = 80)
  private String vin;

  @Column(length = 80)
  private String fuelType;

  @Column(length = 120)
  private String engine;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;
}
