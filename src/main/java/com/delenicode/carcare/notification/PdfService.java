package com.delenicode.carcare.notification;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PdfService {
  public byte[] renderServiceSummary(String title, String body) {
    List<String> lines = new ArrayList<>();
    lines.add(title);
    lines.addAll(body.lines().toList());
    StringBuilder content = new StringBuilder("BT /F1 12 Tf 50 780 Td ");
    for (String line : lines) {
      content.append("(").append(escape(line)).append(") Tj T* ");
    }
    content.append("ET");
    byte[] stream = content.toString().getBytes(StandardCharsets.UTF_8);
    String pdf = """
        %PDF-1.4
        1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj
        2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj
        3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >> endobj
        4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj
        5 0 obj << /Length %d >> stream
        %s
        endstream endobj
        trailer << /Root 1 0 R >>
        %%EOF
        """.formatted(stream.length, content);
    return pdf.getBytes(StandardCharsets.UTF_8);
  }

  private String escape(String value) {
    return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
  }
}
