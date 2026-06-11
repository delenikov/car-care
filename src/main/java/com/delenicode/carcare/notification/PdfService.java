package com.delenicode.carcare.notification;

import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;

@Service
public class PdfService {
  public byte[] renderServiceSummary(String title, String body) {
    return (title + System.lineSeparator() + body).getBytes(StandardCharsets.UTF_8);
  }
}
