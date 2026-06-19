package com.delenicode.carcare;

import static org.assertj.core.api.Assertions.assertThat;

import com.delenicode.carcare.common.LogSanitizer;
import org.junit.jupiter.api.Test;

class LogSanitizerTest {
  @Test
  void emailMasksLocalPartAndKeepsDomainForTroubleshooting() {
    assertThat(LogSanitizer.email("client@example.com")).isEqualTo("c***@example.com");
  }

  @Test
  void emailHandlesInvalidValuesWithoutLeakingInput() {
    assertThat(LogSanitizer.email(null)).isEqualTo("unknown");
    assertThat(LogSanitizer.email("not-an-email")).isEqualTo("invalid-email");
  }

  @Test
  void pathMasksCancellationTokens() {
    assertThat(LogSanitizer.path("/api/appointments/cancel/secret-token-value"))
        .isEqualTo("/api/appointments/cancel/{token}");
    assertThat(LogSanitizer.path("/reservations/cancel/another-secret-token"))
        .isEqualTo("/reservations/cancel/{token}");
  }
}
