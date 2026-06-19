package com.delenicode.carcare.user.repository;


import com.delenicode.carcare.user.model.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
  Optional<AppUser> findByEmail(String email);
  boolean existsByEmail(String email);
}
