package com.delenicode.carcare.auth.dto.response;


import com.delenicode.carcare.auth.model.RefreshToken;
import com.delenicode.carcare.user.dto.response.UserResponse;

public record AuthResponse(String accessToken, String refreshToken, UserResponse user) {
}
