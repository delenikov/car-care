package com.delenicode.carcare.user.model;

import com.delenicode.carcare.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "app_users")
public class AppUser extends BaseEntity {
  @Column(nullable = false, unique = true, length = 160)
  private String email;

  @Column(nullable = false, length = 160)
  private String fullName;

  @Column(nullable = false)
  private String passwordHash;

  @Column(nullable = false)
  private boolean enabled = true;

  @Column(nullable = false)
  private int failedLoginAttempts;

  private Instant lockedUntil;
  private Instant lastLoginAt;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<Role> roles = new HashSet<>();
}
