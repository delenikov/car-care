package com.delenicode.carcare.audit;

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
@Table(name = "audit_events")
public class AuditEvent extends BaseEntity {
  @Column(nullable = false, length = 120)
  private String actor;

  @Column(nullable = false, length = 120)
  private String action;

  @Column(length = 120)
  private String entityType;

  private Long entityId;

  @Column(length = 1000)
  private String details;
}
