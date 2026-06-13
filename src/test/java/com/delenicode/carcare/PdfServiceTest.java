package com.delenicode.carcare;

import static org.assertj.core.api.Assertions.assertThat;

import com.delenicode.carcare.notification.PdfService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PdfServiceTest {
  @Test
  void renderServiceSummaryReturnsPdfBytes() {
    String pdf = new String(new PdfService().renderServiceSummary("Title", "Body"), StandardCharsets.UTF_8);

    assertThat(pdf).startsWith("%PDF-1.4");
    assertThat(pdf).contains("Title");
    assertThat(pdf).contains("Body");
  }
}
