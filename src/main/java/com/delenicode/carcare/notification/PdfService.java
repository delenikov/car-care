package com.delenicode.carcare.notification;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PdfService {
  private static final Color INK = new Color(20, 35, 31);
  private static final Color MUTED = new Color(91, 99, 95);
  private static final Color ACCENT = new Color(242, 207, 122);
  private static final Color PAPER = new Color(255, 250, 240);
  private static final Color BACKGROUND = new Color(245, 239, 226);
  private static final Color BORDER = new Color(227, 216, 196);
  private static final Color DARK_MUTED = new Color(217, 205, 184);
  private static final String[] REGULAR_FONT_CANDIDATES = {
      "C:/Windows/Fonts/arial.ttf",
      "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
      "/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf"
  };
  private static final String[] BOLD_FONT_CANDIDATES = {
      "C:/Windows/Fonts/arialbd.ttf",
      "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
      "/usr/share/fonts/truetype/liberation2/LiberationSans-Bold.ttf"
  };

  public byte[] renderOfferReport(
      String offerId,
      String title,
      String description,
      String issueDate,
      String expiresOn,
      String customerName,
      String customerAddress,
      String customerEmail,
      String vehicle,
      List<String[]> parts,
      String partsCost,
      String laborCost,
      boolean hasDiscount,
      String discountLabel,
      String discountAmount,
      String subtotal,
      String total) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
      PdfWriter writer = PdfWriter.getInstance(doc, out);
      doc.open();

      PdfContentByte canvas = writer.getDirectContentUnder();
      canvas.setColorFill(BACKGROUND);
      canvas.rectangle(0, 0, PageSize.A4.getWidth(), PageSize.A4.getHeight());
      canvas.fill();

      Font brandFont       = font(BOLD_FONT_CANDIDATES,    18, Font.BOLD,   Color.WHITE);
      Font headerSmallFont = font(REGULAR_FONT_CANDIDATES, 10, Font.NORMAL, DARK_MUTED);
      Font offerTitleFont  = font(BOLD_FONT_CANDIDATES,    20, Font.BOLD,   ACCENT);
      Font sectionLabelFont= font(BOLD_FONT_CANDIDATES,     9, Font.BOLD,   MUTED);
      Font bodyFont        = font(REGULAR_FONT_CANDIDATES, 10, Font.NORMAL, MUTED);
      Font bodyBoldFont    = font(BOLD_FONT_CANDIDATES,    10, Font.BOLD,   INK);
      Font tableHeaderFont = font(BOLD_FONT_CANDIDATES,    10, Font.BOLD,   INK);
      Font totalFont       = font(BOLD_FONT_CANDIDATES,    18, Font.BOLD,   INK);

      PdfPTable wrapper = new PdfPTable(1);
      wrapper.setWidthPercentage(100);
      wrapper.getDefaultCell().setBorder(Rectangle.NO_BORDER);

      // Header row: brand name left, "ПОНУДА ЗА СЕРВИС / #id" right
      PdfPTable header = new PdfPTable(new float[]{ 0.52f, 0.48f });
      header.setWidthPercentage(100);
      header.addCell(headerBlock("CarCare", "", Element.ALIGN_LEFT, brandFont, headerSmallFont));
      header.addCell(headerBlock("ПОНУДА ЗА СЕРВИС", "Бр. #" + offerId, Element.ALIGN_RIGHT, offerTitleFont, headerSmallFont));
      wrapper.addCell(block(header, INK, 26, 30, 18, 30));

      // Customer block (left) + offer meta block (right)
      PdfPTable infoTable = new PdfPTable(new float[]{ 0.5f, 0.5f });
      infoTable.setWidthPercentage(100);
      infoTable.addCell(customerBlock(customerName, customerAddress, customerEmail, sectionLabelFont, bodyBoldFont, bodyFont));
      infoTable.addCell(offerMetaBlock(issueDate, expiresOn, vehicle, bodyFont, bodyBoldFont));
      wrapper.addCell(block(infoTable, PAPER, 24, 30, 10, 30));

      // Title + description
      PdfPTable descTable = new PdfPTable(new float[]{ 0.32f, 0.68f });
      descTable.setWidthPercentage(100);
      descTable.addCell(tableHeader("Поле", tableHeaderFont));
      descTable.addCell(tableHeader("Информација", tableHeaderFont));
      descTable.addCell(detailLabel("Наслов", bodyBoldFont));
      descTable.addCell(detailValue(title, bodyFont));
      if (description != null && !description.isBlank()) {
        descTable.addCell(detailLabel("Опис", bodyBoldFont));
        descTable.addCell(detailValue(description, bodyFont));
      }
      wrapper.addCell(block(descTable, PAPER, 10, 30, 0, 30));

      // Parts table (shown only when the offer has individual parts)
      if (!parts.isEmpty()) {
        PdfPTable partsTable = new PdfPTable(new float[]{ 0.72f, 0.28f });
        partsTable.setWidthPercentage(100);
        partsTable.addCell(tableHeader("Дел / услуга", tableHeaderFont));
        partsTable.addCell(tableHeaderRight("Цена", tableHeaderFont));
        for (String[] part : parts) {
          partsTable.addCell(detailLabel(part[0], bodyBoldFont));
          partsTable.addCell(detailValueRight(part[1], bodyFont));
        }
        wrapper.addCell(block(partsTable, PAPER, 12, 30, 0, 30));
      }

      // Cost breakdown
      PdfPTable totalsTable = new PdfPTable(new float[]{ 0.6f, 0.4f });
      totalsTable.setWidthPercentage(100);
      totalsTable.addCell(costLabel("Цена на делови", bodyBoldFont));
      totalsTable.addCell(costValue(partsCost, bodyFont));
      totalsTable.addCell(costLabel("Цена на работа", bodyBoldFont));
      totalsTable.addCell(costValue(laborCost, bodyFont));
      if (hasDiscount) {
        totalsTable.addCell(costLabel("Меѓусума", bodyBoldFont));
        totalsTable.addCell(costValue(subtotal, bodyFont));
        totalsTable.addCell(costLabel(discountLabel, bodyBoldFont));
        totalsTable.addCell(costValue("- " + discountAmount, bodyFont));
      }
      totalsTable.addCell(totalLabel("ВКУПНО", totalFont));
      totalsTable.addCell(totalValue(total, totalFont));
      wrapper.addCell(block(totalsTable, PAPER, 12, 30, 28, 30));

      // Footer note
      String footerText = expiresOn != null
          ? "Оваа понуда е важечка до " + expiresOn + ". Ви благодариме за довербата."
          : "Ви благодариме за довербата.";
      PdfPCell footerCell = new PdfPCell(new Phrase(footerText, bodyFont));
      footerCell.setBorder(Rectangle.NO_BORDER);
      footerCell.setPaddingTop(0);
      footerCell.setPaddingRight(30);
      footerCell.setPaddingBottom(26);
      footerCell.setPaddingLeft(30);
      footerCell.setBackgroundColor(PAPER);
      wrapper.addCell(footerCell);

      doc.add(wrapper);
      doc.close();
      return out.toByteArray();
    } catch (Exception ex) {
      throw new IllegalStateException("PDF generation failed", ex);
    }
  }

  public byte[] renderServiceSummary(String title, String body) {
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Document document = new Document(PageSize.A4, 42, 42, 42, 42);
      PdfWriter.getInstance(document, output);
      document.open();

      Font titleFont = font(BOLD_FONT_CANDIDATES, 20, Font.BOLD, INK);
      Font labelFont = font(BOLD_FONT_CANDIDATES, 10, Font.BOLD, INK);
      Font valueFont = font(REGULAR_FONT_CANDIDATES, 10, Font.NORMAL, MUTED);

      Paragraph heading = new Paragraph(title, titleFont);
      heading.setSpacingAfter(18);
      document.add(heading);

      PdfPTable table = new PdfPTable(new float[] { 0.34f, 0.66f });
      table.setWidthPercentage(100);
      table.setSpacingAfter(18);
      table.getDefaultCell().setBorder(Rectangle.NO_BORDER);

      for (String line : body.lines().toList()) {
        if (line.isBlank()) {
          continue;
        }
        int separator = line.indexOf(':');
        if (separator > 0) {
          addRow(table, line.substring(0, separator).trim(), line.substring(separator + 1).trim(), labelFont, valueFont);
        } else {
          PdfPCell cell = new PdfPCell(new Phrase(line, valueFont));
          cell.setColspan(2);
          cell.setPadding(10);
          cell.setBorderColor(new Color(227, 216, 196));
          cell.setBackgroundColor(PAPER);
          table.addCell(cell);
        }
      }

      document.add(table);
      document.close();
      return output.toByteArray();
    } catch (Exception ex) {
      throw new IllegalStateException("PDF generation failed", ex);
    }
  }

  public byte[] renderServiceReport(
      String documentNumber,
      String customerName,
      String customerAddress,
      String customerEmail,
      String serviceDate,
      String vehicle,
      String odometer,
      String serviceType,
      String replacedParts,
      String notes,
      String partsCost,
      String laborCost,
      String total
  ) {
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Document document = new Document(PageSize.A4, 36, 36, 36, 36);
      PdfWriter writer = PdfWriter.getInstance(document, output);
      document.open();

      PdfContentByte canvas = writer.getDirectContentUnder();
      canvas.setColorFill(BACKGROUND);
      canvas.rectangle(0, 0, PageSize.A4.getWidth(), PageSize.A4.getHeight());
      canvas.fill();

      Font brandFont = font(BOLD_FONT_CANDIDATES, 18, Font.BOLD, Color.WHITE);
      Font headerSmallFont = font(REGULAR_FONT_CANDIDATES, 10, Font.NORMAL, DARK_MUTED);
      Font reportTitleFont = font(BOLD_FONT_CANDIDATES, 20, Font.BOLD, ACCENT);
      Font sectionLabelFont = font(BOLD_FONT_CANDIDATES, 9, Font.BOLD, MUTED);
      Font bodyFont = font(REGULAR_FONT_CANDIDATES, 10, Font.NORMAL, MUTED);
      Font bodyBoldFont = font(BOLD_FONT_CANDIDATES, 10, Font.BOLD, INK);
      Font tableHeaderFont = font(BOLD_FONT_CANDIDATES, 10, Font.BOLD, INK);
      Font totalFont = font(BOLD_FONT_CANDIDATES, 18, Font.BOLD, INK);

      PdfPTable wrapper = new PdfPTable(1);
      wrapper.setWidthPercentage(100);
      wrapper.getDefaultCell().setBorder(Rectangle.NO_BORDER);

      PdfPTable header = new PdfPTable(new float[] { 0.52f, 0.48f });
      header.setWidthPercentage(100);
      header.addCell(headerBlock("CarCare", "", Element.ALIGN_LEFT, brandFont, headerSmallFont));
      header.addCell(headerBlock("СЕРВИСЕН ИЗВЕШТАЈ", "Бр. #" + documentNumber, Element.ALIGN_RIGHT, reportTitleFont, headerSmallFont));
      wrapper.addCell(block(header, INK, 26, 30, 18, 30));

      PdfPTable info = new PdfPTable(new float[] { 0.5f, 0.5f });
      info.setWidthPercentage(100);
      info.addCell(customerBlock(customerName, customerAddress, customerEmail, sectionLabelFont, bodyBoldFont, bodyFont));
      info.addCell(serviceInfoBlock(serviceDate, vehicle, odometer, bodyFont, bodyBoldFont));
      wrapper.addCell(block(info, PAPER, 24, 30, 10, 30));

      PdfPTable details = new PdfPTable(new float[] { 0.35f, 0.65f });
      details.setWidthPercentage(100);
      details.addCell(tableHeader("Поле", tableHeaderFont));
      details.addCell(tableHeader("Информација", tableHeaderFont));
      details.addCell(detailLabel("Тип на сервис", bodyBoldFont));
      details.addCell(detailValue(serviceType, bodyFont));
      details.addCell(detailLabel("Заменети делови", bodyBoldFont));
      details.addCell(detailValue(replacedParts, bodyFont));
      details.addCell(detailLabel("Белешки", bodyBoldFont));
      details.addCell(detailValue(notes, bodyFont));
      wrapper.addCell(block(details, PAPER, 10, 30, 0, 30));

      PdfPTable totals = new PdfPTable(new float[] { 0.55f, 0.45f });
      totals.setWidthPercentage(100);
      totals.addCell(costLabel("Цена на делови", bodyBoldFont));
      totals.addCell(costValue(partsCost, bodyFont));
      totals.addCell(costLabel("Цена на работа", bodyBoldFont));
      totals.addCell(costValue(laborCost, bodyFont));
      totals.addCell(totalLabel("ВКУПНО", totalFont));
      totals.addCell(totalValue(total, totalFont));
      wrapper.addCell(block(totals, PAPER, 12, 30, 28, 30));

      PdfPCell note = new PdfPCell(new Phrase("Ви благодариме за довербата. Овој извештај ги содржи податоците за завршениот сервис.", bodyFont));
      note.setBorder(Rectangle.NO_BORDER);
      note.setPaddingTop(0);
      note.setPaddingRight(30);
      note.setPaddingBottom(26);
      note.setPaddingLeft(30);
      note.setBackgroundColor(PAPER);
      wrapper.addCell(note);

      document.add(wrapper);
      document.close();
      return output.toByteArray();
    } catch (Exception ex) {
      throw new IllegalStateException("PDF generation failed", ex);
    }
  }

  private void addRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
    PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
    labelCell.setPadding(10);
    labelCell.setBorderColor(new Color(227, 216, 196));
    labelCell.setBackgroundColor(label.equalsIgnoreCase("Вкупно") || label.equalsIgnoreCase("TOTAL") ? ACCENT : PAPER);
    labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
    table.addCell(labelCell);

    PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
    valueCell.setPadding(10);
    valueCell.setBorderColor(new Color(227, 216, 196));
    valueCell.setBackgroundColor(labelCell.getBackgroundColor());
    valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
    table.addCell(valueCell);
  }

  private PdfPCell block(PdfPTable table, Color background, float top, float right, float bottom, float left) {
    PdfPCell cell = new PdfPCell(table);
    cell.setBorder(Rectangle.NO_BORDER);
    cell.setBackgroundColor(background);
    cell.setPaddingTop(top);
    cell.setPaddingRight(right);
    cell.setPaddingBottom(bottom);
    cell.setPaddingLeft(left);
    return cell;
  }

  private PdfPCell headerBlock(String title, String subtitle, int alignment, Font titleFont, Font subtitleFont) {
    Paragraph content = new Paragraph();
    Paragraph titleParagraph = new Paragraph(title, titleFont);
    titleParagraph.setAlignment(alignment);
    titleParagraph.setSpacingAfter(6);
    content.add(titleParagraph);
    if (!subtitle.isBlank()) {
      Paragraph subtitleParagraph = new Paragraph(subtitle, subtitleFont);
      subtitleParagraph.setAlignment(alignment);
      content.add(subtitleParagraph);
    }
    PdfPCell cell = new PdfPCell(content);
    cell.setBorder(Rectangle.NO_BORDER);
    cell.setBackgroundColor(INK);
    cell.setVerticalAlignment(Element.ALIGN_TOP);
    return cell;
  }

  private PdfPCell customerBlock(String name, String address, String email, Font labelFont, Font nameFont, Font valueFont) {
    Paragraph content = new Paragraph();
    content.add(new Paragraph("Клиент", labelFont));
    content.add(new Paragraph(name, nameFont));
    content.add(new Paragraph(address, valueFont));
    content.add(new Paragraph(email, valueFont));
    PdfPCell cell = new PdfPCell(content);
    cell.setBorder(Rectangle.NO_BORDER);
    cell.setBackgroundColor(PAPER);
    cell.setPaddingRight(12);
    return cell;
  }

  private PdfPCell serviceInfoBlock(String serviceDate, String vehicle, String odometer, Font labelFont, Font valueFont) {
    PdfPTable table = new PdfPTable(new float[] { 0.42f, 0.58f });
    table.setWidthPercentage(100);
    serviceInfoRow(table, "Датум на сервис:", serviceDate, labelFont, valueFont);
    serviceInfoRow(table, "Возило:", vehicle, labelFont, valueFont);
    serviceInfoRow(table, "Километража:", odometer + " km", labelFont, valueFont);
    PdfPCell cell = new PdfPCell(table);
    cell.setBorder(Rectangle.NO_BORDER);
    cell.setBackgroundColor(PAPER);
    return cell;
  }

  private void serviceInfoRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
    table.addCell(noBorder(label, labelFont, Element.ALIGN_LEFT));
    table.addCell(noBorder(value, valueFont, Element.ALIGN_RIGHT));
  }

  private PdfPCell noBorder(String value, Font font, int alignment) {
    PdfPCell cell = new PdfPCell(new Phrase(value, font));
    cell.setBorder(Rectangle.NO_BORDER);
    cell.setPaddingBottom(6);
    cell.setHorizontalAlignment(alignment);
    return cell;
  }

  private PdfPCell tableHeader(String value, Font font) {
    PdfPCell cell = new PdfPCell(new Phrase(value, font));
    cell.setBackgroundColor(ACCENT);
    cell.setBorderColor(ACCENT);
    cell.setPadding(10);
    return cell;
  }

  private PdfPCell detailLabel(String value, Font font) {
    PdfPCell cell = new PdfPCell(new Phrase(value, font));
    cell.setBorderColor(BORDER);
    cell.setPadding(10);
    cell.setBackgroundColor(PAPER);
    return cell;
  }

  private PdfPCell detailValue(String value, Font font) {
    PdfPCell cell = new PdfPCell(new Phrase(value, font));
    cell.setBorderColor(BORDER);
    cell.setPadding(10);
    cell.setBackgroundColor(PAPER);
    return cell;
  }

  private PdfPCell costLabel(String value, Font font) {
    PdfPCell cell = new PdfPCell(new Phrase(value, font));
    cell.setBorder(Rectangle.TOP);
    cell.setBorderColor(BORDER);
    cell.setPaddingTop(10);
    cell.setPaddingBottom(6);
    cell.setBackgroundColor(PAPER);
    return cell;
  }

  private PdfPCell costValue(String value, Font font) {
    PdfPCell cell = costLabel(value, font);
    cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
    return cell;
  }

  private PdfPCell totalLabel(String value, Font font) {
    PdfPCell cell = new PdfPCell(new Phrase(value, font));
    cell.setBorder(Rectangle.TOP);
    cell.setBorderColor(ACCENT);
    cell.setBorderWidthTop(2);
    cell.setPaddingTop(12);
    cell.setBackgroundColor(PAPER);
    return cell;
  }

  private PdfPCell totalValue(String value, Font font) {
    PdfPCell cell = totalLabel(value, font);
    cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
    return cell;
  }

  private Font font(String[] candidates, int size, int style, Color color) throws Exception {
    for (String candidate : candidates) {
      if (new File(candidate).isFile()) {
        BaseFont baseFont = BaseFont.createFont(candidate, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        return new Font(baseFont, size, style, color);
      }
    }
    return new Font(Font.HELVETICA, size, style, color);
  }

  private PdfPCell offerMetaBlock(String issueDate, String expiresOn, String vehicle, Font labelFont, Font valueFont) {
    PdfPTable table = new PdfPTable(new float[]{ 0.42f, 0.58f });
    table.setWidthPercentage(100);
    serviceInfoRow(table, "Датум на понуда:", issueDate, labelFont, valueFont);
    if (expiresOn != null) {
      serviceInfoRow(table, "Важи до:", expiresOn, labelFont, valueFont);
    }
    serviceInfoRow(table, "Возило:", vehicle, labelFont, valueFont);
    PdfPCell cell = new PdfPCell(table);
    cell.setBorder(Rectangle.NO_BORDER);
    cell.setBackgroundColor(PAPER);
    return cell;
  }

  private PdfPCell tableHeaderRight(String value, Font font) {
    PdfPCell cell = tableHeader(value, font);
    cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
    return cell;
  }

  private PdfPCell detailValueRight(String value, Font font) {
    PdfPCell cell = detailValue(value, font);
    cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
    return cell;
  }
}
