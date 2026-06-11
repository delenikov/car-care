package com.delenicode.carcare.auth;

import com.delenicode.carcare.user.UserResponse;

public record AuthResponse(String accessToken, String refreshToken, UserResponse user) {
}
