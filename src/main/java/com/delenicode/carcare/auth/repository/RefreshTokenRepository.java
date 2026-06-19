package com.delenicode.carcare.auth.repository;


import com.delenicode.carcare.auth.model.RefreshToken;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  Optional<RefreshToken> findByTokenAndRevokedFalse(String token);
  List<RefreshToken> findAllByUserIdAndRevokedFalse(Long userId);
}
