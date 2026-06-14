package com.delenicode.carcare.user;

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
@Table(name = "roles")
public class Role extends BaseEntity {
  @Column(nullable = false, unique = true, length = 80)
  private String name;

  public Role(String name) {
    this.name = name;
  }
}
