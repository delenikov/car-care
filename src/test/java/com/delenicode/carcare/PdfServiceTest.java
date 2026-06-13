package com.delenicode.carcare;

import static org.assertj.core.api.Assertions.assertThat;

import com.delenicode.carcare.notification.PdfService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PdfServiceTest {
  @Test
  void renderServiceSummaryReturnsPdfBytes() {
    String pdf = new String(new PdfService().renderServiceSummary("Сервисен извештај", "Клиент: Стојан\nВкупно: 2.500,00 ден."), StandardCharsets.ISO_8859_1);

    assertThat(pdf).startsWith("%PDF-");
    assertThat(pdf).contains("/Font");
    assertThat(pdf).contains("%%EOF");
  }
}
