package com.delenicode.carcare.loyalty;

import com.delenicode.carcare.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "loyalty_rules")
public class LoyaltyRule extends BaseEntity {
  @Column(nullable = false, length = 160)
  private String name;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal pointsPerCurrencyUnit;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal discountPercent;

  @Column(nullable = false)
  private boolean active = true;
}
