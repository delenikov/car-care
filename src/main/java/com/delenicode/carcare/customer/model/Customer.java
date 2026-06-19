package com.delenicode.carcare.customer.model;

import com.delenicode.carcare.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {
  @Column(nullable = false, length = 160)
  private String fullName;

  @Column(nullable = false, length = 80)
  private String firstName;

  @Column(nullable = false, length = 80)
  private String lastName;

  @Column(nullable = false, unique = true, length = 160)
  private String email;

  @Column(length = 40)
  private String phone;

  @Column(length = 300)
  private String address;

  @Column(nullable = false)
  private boolean deleted;
}
