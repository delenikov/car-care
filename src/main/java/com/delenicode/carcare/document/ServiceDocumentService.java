package com.delenicode.carcare.document;

import com.delenicode.carcare.customer.CustomerRepository;
import com.delenicode.carcare.notification.EmailDeliveryResult;
import com.delenicode.carcare.notification.EmailService;
import com.delenicode.carcare.notification.PdfService;
import com.delenicode.carcare.servicerecord.ServiceRecord;
import com.delenicode.carcare.servicerecord.ServiceRecordRepository;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServiceDocumentService {
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
  private static final Locale MK_LOCALE = Locale.forLanguageTag("mk-MK");

  private final ServiceDocumentRepository documents;
  private final CustomerRepository customers;
  private final ServiceRecordRepository serviceRecords;
  private final EmailService emailService;
  private final PdfService pdfService;

  @Transactional(readOnly = true)
  public List<ServiceDocumentResponse> findAll() {
    return documents.findAllByOrderByCreatedAtDescIdDesc().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public ServiceDocumentResponse findById(Long id) {
    return documents.findById(id).map(this::toResponse).orElseThrow(() -> new IllegalArgumentException("Document not found"));
  }

  @Transactional
  public ServiceDocumentResponse create(ServiceDocumentRequest request) {
    ServiceDocument document = new ServiceDocument();
    document.setCustomer(customers.findById(request.customerId()).orElseThrow(() -> new IllegalArgumentException("Customer not found")));
    if (request.serviceRecordId() != null) {
      document.setServiceRecord(serviceRecords.findById(request.serviceRecordId()).orElseThrow(() -> new IllegalArgumentException("Service record not found")));
    }
    document.setType(request.type() == null ? DocumentType.OTHER : request.type());
    document.setFileName(request.fileName());
    document.setContentType(request.contentType());
    document.setStorageKey(request.storageKey());
    return toResponse(documents.save(document));
  }

  @Transactional
  public ServiceDocumentResponse generateForServiceRecord(ServiceRecord record) {
    ServiceDocument document = new ServiceDocument();
    document.setCustomer(record.getCustomer());
    document.setServiceRecord(record);
    document.setType(DocumentType.INSPECTION);
    document.setFileName("service-record-" + record.getId() + ".pdf");
    document.setContentType("application/pdf");
    document.setStorageKey("generated/service-record-" + record.getId() + ".pdf");
    return toResponse(documents.save(document));
  }

  @Transactional
  public ServiceDocumentResponse send(Long id) {
    ServiceDocument document = documents.findById(id).orElseThrow(() -> new IllegalArgumentException("Document not found"));
    ServiceRecord record = document.getServiceRecord();
    String subject = record == null ? "Сервисен документ" : "Извештај за завршен сервис: " + record.getServiceType();
    EmailDeliveryResult result = emailService.sendHtmlWithAttachment(document.getCustomer().getEmail(), subject, documentHtmlBody(document), documentBody(document), document.getFileName(), document.getContentType(), exportPdf(id));
    if (!result.accepted()) {
      throw new IllegalStateException(result.message());
    }
    return toResponse(document);
  }

  @Transactional(readOnly = true)
  public byte[] exportPdf(Long id) {
    ServiceDocument document = documents.findById(id).orElseThrow(() -> new IllegalArgumentException("Document not found"));
    ServiceRecord record = document.getServiceRecord();
    if (record == null) {
      return pdfService.renderServiceSummary(document.getFileName(), documentBody(document));
    }
    return pdfService.renderServiceReport(
        String.valueOf(record.getId()),
        document.getCustomer().getFullName(),
        blankToDash(document.getCustomer().getAddress()),
        document.getCustomer().getEmail(),
        DATE_FORMAT.format(record.getServiceDate()),
        vehicleText(record),
        record.getOdometer() == null ? "-" : NumberFormat.getIntegerInstance(MK_LOCALE).format(record.getOdometer()),
        record.getServiceType(),
        blankToDash(record.getReplacedParts()),
        blankToDash(record.getNotes()),
        formatMoney(record.getPartsCost()),
        formatMoney(record.getLaborCost()),
        formatMoney(record.getTotalAmount())
    );
  }

  public ServiceDocumentResponse toResponse(ServiceDocument document) {
    ServiceRecord record = document.getServiceRecord();
    return new ServiceDocumentResponse(document.getId(), document.getCustomer().getId(), record == null ? null : record.getId(), document.getType(), document.getFileName(), document.getContentType(), document.getStorageKey(), document.getCreatedAt());
  }

  private String documentBody(ServiceDocument document) {
    ServiceRecord record = document.getServiceRecord();
    if (record == null) {
      return "Документ за " + document.getCustomer().getFullName();
    }
    return "Клиент: " + document.getCustomer().getFullName()
        + "\nВозило: " + vehicleText(record)
        + "\nДатум на сервис: " + DATE_FORMAT.format(record.getServiceDate())
        + "\nТип на сервис: " + record.getServiceType()
        + "\nКилометража: " + record.getOdometer() + " km"
        + "\nЗаменети делови: " + blankToDash(record.getReplacedParts())
        + "\nЦена на делови: " + formatMoney(record.getPartsCost())
        + "\nЦена на работа: " + formatMoney(record.getLaborCost())
        + "\nВкупно: " + formatMoney(record.getTotalAmount())
        + "\nБелешки: " + blankToDash(record.getNotes());
  }

  private String documentHtmlBody(ServiceDocument document) {
    ServiceRecord record = document.getServiceRecord();
    if (record == null) {
      return genericDocumentHtml(document);
    }
    return """
        <!doctype html>
        <html lang="mk">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Извештај за сервис</title>
        </head>
        <body style="margin:0;padding:0;background:#f5efe2;font-family:Arial,Helvetica,sans-serif;color:#26322f;">
          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f5efe2;padding:32px 12px;">
            <tr>
              <td align="center">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:760px;background:#fffaf0;border:1px solid #e3d8c4;border-radius:8px;overflow:hidden;">
                  <tr>
                    <td style="padding:34px 38px 22px;background:#14231f;color:#fffaf0;">
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                        <tr>
                          <td style="vertical-align:top;">
                            <div style="font-size:22px;font-weight:700;color:#fffaf0;">CarCare ASMS</div>
                            <div style="font-size:13px;color:#d9cdb8;margin-top:8px;">Авто сервис центар</div>
                          </td>
                          <td align="right" style="vertical-align:top;">
                            <div style="font-size:26px;font-weight:700;color:#f2cf7a;">СЕРВИСЕН ИЗВЕШТАЈ</div>
                            <div style="font-size:13px;color:#d9cdb8;margin-top:8px;">Бр. #%s</div>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:28px 38px 16px;">
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                        <tr>
                          <td width="50%%" style="vertical-align:top;padding-right:20px;">
                            <div style="font-size:12px;text-transform:uppercase;color:#7a6c58;font-weight:700;margin-bottom:8px;">Клиент</div>
                            <div style="font-size:16px;font-weight:700;color:#14231f;">%s</div>
                            <div style="font-size:14px;line-height:1.6;color:#5b635f;margin-top:6px;">%s<br>%s</div>
                          </td>
                          <td width="50%%" style="vertical-align:top;">
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="font-size:14px;color:#5b635f;">
                              <tr><td style="padding:4px 0;">Датум на сервис:</td><td align="right" style="padding:4px 0;color:#14231f;font-weight:700;">%s</td></tr>
                              <tr><td style="padding:4px 0;">Возило:</td><td align="right" style="padding:4px 0;color:#14231f;font-weight:700;">%s</td></tr>
                              <tr><td style="padding:4px 0;">Километража:</td><td align="right" style="padding:4px 0;color:#14231f;font-weight:700;">%s km</td></tr>
                            </table>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:14px 38px 0;">
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="border-collapse:collapse;font-size:14px;">
                        <tr style="background:#f2cf7a;color:#14231f;">
                          <th align="left" style="padding:13px 12px;border-bottom:1px solid #d9a520;">Поле</th>
                          <th align="left" style="padding:13px 12px;border-bottom:1px solid #d9a520;">Информација</th>
                        </tr>
                        %s
                      </table>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:0 38px 36px;">
                      <div style="border-top:1px solid #e3d8c4;margin-top:14px;margin-bottom:14px;"></div>
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="font-size:15px;color:#26322f;">
                        <tr><td style="padding:6px 0;font-weight:700;">Цена на делови</td><td align="right" style="padding:6px 0;">%s</td></tr>
                        <tr><td style="padding:6px 0;font-weight:700;">Цена на работа</td><td align="right" style="padding:6px 0;">%s</td></tr>
                      </table>
                      <div style="border-top:2px solid #d9a520;margin:14px 0;"></div>
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                        <tr>
                          <td style="font-size:24px;font-weight:700;color:#14231f;">ВКУПНО</td>
                          <td align="right" style="font-size:24px;font-weight:700;color:#14231f;">%s</td>
                        </tr>
                      </table>
                      <div style="font-size:13px;line-height:1.6;color:#6f756f;margin-top:22px;">Ви благодариме за довербата. Овој извештај ги содржи податоците за завршениот сервис.</div>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
          </table>
        </body>
        </html>
        """.formatted(
        record.getId(),
        escape(document.getCustomer().getFullName()),
        escape(blankToDash(document.getCustomer().getAddress())),
        escape(document.getCustomer().getEmail()),
        DATE_FORMAT.format(record.getServiceDate()),
        escape(vehicleText(record)),
        record.getOdometer() == null ? "-" : NumberFormat.getIntegerInstance(MK_LOCALE).format(record.getOdometer()),
        serviceRows(record),
        formatMoney(record.getPartsCost()),
        formatMoney(record.getLaborCost()),
        formatMoney(record.getTotalAmount())
    );
  }

  private String serviceRows(ServiceRecord record) {
    return row("Тип на сервис", record.getServiceType())
        + row("Заменети делови", blankToDash(record.getReplacedParts()))
        + row("Белешки", blankToDash(record.getNotes()));
  }

  private String row(String label, String value) {
    return """
        <tr>
          <td style="padding:14px 12px;border-bottom:1px solid #efe5d4;color:#5b635f;font-weight:700;">%s</td>
          <td style="padding:14px 12px;border-bottom:1px solid #efe5d4;color:#26322f;">%s</td>
        </tr>
        """.formatted(escape(label), escape(value));
  }

  private String genericDocumentHtml(ServiceDocument document) {
    return """
        <!doctype html>
        <html lang="mk">
        <body style="margin:0;padding:24px;background:#f5efe2;font-family:Arial,Helvetica,sans-serif;color:#26322f;">
          <div style="max-width:640px;margin:0 auto;background:#fffaf0;border:1px solid #e3d8c4;border-radius:8px;padding:28px;">
            <h1 style="margin:0 0 12px;color:#14231f;">Сервисен документ</h1>
            <p style="margin:0;color:#5b635f;">Документ за %s.</p>
          </div>
        </body>
        </html>
        """.formatted(escape(document.getCustomer().getFullName()));
  }

  private String vehicleText(ServiceRecord record) {
    return record.getVehicle().getPlateNumber() + " - " + record.getVehicle().getMake() + " " + record.getVehicle().getModel();
  }

  private String formatMoney(BigDecimal value) {
    NumberFormat format = NumberFormat.getNumberInstance(MK_LOCALE);
    format.setMinimumFractionDigits(2);
    format.setMaximumFractionDigits(2);
    return format.format(value == null ? BigDecimal.ZERO : value) + " ден.";
  }

  private String blankToDash(String value) {
    return value == null || value.isBlank() ? "-" : value;
  }

  private String escape(String value) {
    return blankToDash(value)
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
}
