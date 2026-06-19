package com.delenicode.carcare.auth.dto.request;


import com.delenicode.carcare.auth.model.RefreshToken;
import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank String refreshToken) {
}
