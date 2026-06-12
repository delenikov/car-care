package com.delenicode.carcare.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  Optional<RefreshToken> findByTokenAndRevokedFalse(String token);
  java.util.List<RefreshToken> findAllByUserIdAndRevokedFalse(Long userId);
}
