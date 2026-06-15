package com.delenicode.carcare.offer;

import com.delenicode.carcare.common.BaseEntity;
import com.delenicode.carcare.customer.Customer;
import com.delenicode.carcare.vehicle.Vehicle;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "offers")
public class Offer extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vehicle_id")
  private Vehicle vehicle;

  @Column(nullable = false, length = 160)
  private String title;

  @Column(length = 2000)
  private String description;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal subtotalAmount;

  @Column(nullable = false, precision = 5, scale = 2)
  private BigDecimal discountPercent;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal discountAmount;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal partsCost;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal laborCost;

  @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("position ASC")
  private List<OfferPart> parts = new ArrayList<>();

  private LocalDate expiresOn;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private OfferStatus status = OfferStatus.DRAFT;
}
