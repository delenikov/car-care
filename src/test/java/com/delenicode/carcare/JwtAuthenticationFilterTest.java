package com.delenicode.carcare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.delenicode.carcare.security.CarcareUserDetailsService;
import com.delenicode.carcare.security.JwtAuthenticationFilter;
import com.delenicode.carcare.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
  @Mock
  JwtService jwtService;
  @Mock
  CarcareUserDetailsService userDetailsService;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void invalidBearerTokenReturnsUnauthorizedSoRefreshCanRun() throws Exception {
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/users");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer expired-token");
    when(jwtService.isValid("expired-token")).thenThrow(new RuntimeException("Expired JWT"));

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(userDetailsService, never()).loadUserByUsername(org.mockito.ArgumentMatchers.anyString());
  }
}
