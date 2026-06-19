package com.delenicode.carcare.document.model;

import com.delenicode.carcare.common.BaseEntity;
import com.delenicode.carcare.customer.model.Customer;
import com.delenicode.carcare.servicerecord.model.ServiceRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "service_documents")
public class ServiceDocument extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "service_record_id")
  private ServiceRecord serviceRecord;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private DocumentType type = DocumentType.OTHER;

  @Column(nullable = false, length = 200)
  private String fileName;

  @Column(nullable = false, length = 120)
  private String contentType;

  @Column(nullable = false, length = 500)
  private String storageKey;
}
