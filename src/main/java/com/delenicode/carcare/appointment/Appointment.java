package com.delenicode.carcare.appointment;

import com.delenicode.carcare.common.BaseEntity;
import com.delenicode.carcare.customer.Customer;
import com.delenicode.carcare.vehicle.Vehicle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "appointments")
public class Appointment extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vehicle_id", nullable = false)
  private Vehicle vehicle;

  @Column(nullable = false)
  private OffsetDateTime scheduledAt;

  @Column(nullable = false, length = 160)
  private String serviceType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private AppointmentStatus status = AppointmentStatus.SCHEDULED;

  @Column(length = 1000)
  private String notes;
}
