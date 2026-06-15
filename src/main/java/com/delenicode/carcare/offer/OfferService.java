package com.delenicode.carcare.offer;

import com.delenicode.carcare.customer.Customer;
import com.delenicode.carcare.customer.CustomerRepository;
import com.delenicode.carcare.loyalty.CustomerLoyaltyService;
import com.delenicode.carcare.notification.EmailDeliveryResult;
import com.delenicode.carcare.notification.EmailService;
import com.delenicode.carcare.notification.PdfService;
import com.delenicode.carcare.vehicle.Vehicle;
import com.delenicode.carcare.vehicle.VehicleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OfferService {
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
  private static final Locale MK_LOCALE = Locale.forLanguageTag("mk-MK");
  private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

  private final OfferRepository offers;
  private final CustomerRepository customers;
  private final VehicleRepository vehicles;
  private final EmailService emailService;
  private final PdfService pdfService;
  private final CustomerLoyaltyService loyalty;

  @Transactional(readOnly = true)
  public List<OfferResponse> findAll() {
    return offers.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public OfferResponse findById(Long id) {
    return offers.findById(id).map(this::toResponse).orElseThrow(() -> new IllegalArgumentException("Offer not found"));
  }

  @Transactional
  public OfferResponse create(OfferRequest request) {
    List<OfferPartRequest> requestedParts = request.parts() == null ? List.of() : request.parts();
    validateParts(requestedParts);
    BigDecimal partsCost = requestedParts.isEmpty() ? costOrZero(request.partsCost()) : requestedParts.stream().map(OfferPartRequest::price).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal laborCost = costOrFallback(request.laborCost(), request.amount());
    rejectNegative(partsCost, "Parts cost must not be negative");
    rejectNegative(laborCost, "Labor cost must not be negative");
    Offer offer = new Offer();
    Customer customer = customers.findByIdAndDeletedFalse(request.customerId()).orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    BigDecimal subtotalAmount = partsCost.add(laborCost);
    BigDecimal discountPercent = loyalty.discountPercentForCustomer(customer.getId());
    BigDecimal discountAmount = discountAmount(subtotalAmount, discountPercent);
    offer.setCustomer(customer);
    if (request.vehicleId() != null) {
      Vehicle vehicle = vehicles.findById(request.vehicleId()).filter(existing -> !existing.getCustomer().isDeleted()).orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
      if (!Objects.equals(vehicle.getCustomer().getId(), customer.getId())) {
        throw new IllegalArgumentException("Vehicle does not belong to customer");
      }
      offer.setVehicle(vehicle);
    }
    offer.setTitle(request.title());
    offer.setDescription(request.description());
    offer.setPartsCost(partsCost);
    offer.setLaborCost(laborCost);
    offer.setSubtotalAmount(subtotalAmount);
    offer.setDiscountPercent(discountPercent);
    offer.setDiscountAmount(discountAmount);
    offer.setAmount(subtotalAmount.subtract(discountAmount));
    offer.setExpiresOn(request.expiresOn());
    for (int index = 0; index < requestedParts.size(); index++) {
      OfferPartRequest requestedPart = requestedParts.get(index);
      addPart(offer, requestedPart, index);
    }
    Offer saved = offers.save(offer);
    deliverOffer(saved);
    saved.setStatus(OfferStatus.SENT);
    return toResponse(saved);
  }

  @Transactional
  public OfferResponse send(Long id) {
    Offer offer = offers.findById(id).orElseThrow(() -> new IllegalArgumentException("Offer not found"));
    deliverOffer(offer);
    offer.setStatus(OfferStatus.SENT);
    return toResponse(offers.save(offer));
  }

  @Transactional(readOnly = true)
  public byte[] exportPdf(Long id) {
    Offer offer = offers.findById(id).orElseThrow(() -> new IllegalArgumentException("Offer not found"));
    return pdfService.renderServiceSummary("Понуда за сервис: " + offer.getTitle(), offerBody(offer));
  }

  public OfferResponse toResponse(Offer offer) {
    Vehicle vehicle = offer.getVehicle();
    List<OfferPartResponse> parts = offer.getParts().stream().map(part -> new OfferPartResponse(part.getName(), part.getPrice())).toList();
    return new OfferResponse(
        offer.getId(),
        offer.getCustomer().getId(),
        offer.getCustomer().getFullName(),
        vehicle == null ? null : vehicle.getId(),
        vehicle == null ? null : vehicle.getPlateNumber(),
        vehicle == null ? null : vehicle.getMake() + " " + vehicle.getModel(),
        offer.getTitle(),
        offer.getDescription(),
        parts,
        offer.getPartsCost(),
        offer.getLaborCost(),
        subtotalAmount(offer),
        discountPercent(offer),
        discountAmount(offer),
        amount(offer),
        offer.getExpiresOn(),
        offer.getStatus());
  }

  private String offerBody(Offer offer) {
    StringBuilder body = new StringBuilder();
    body.append("Понуда: ").append(offer.getTitle()).append("\n\n");
    if (!offer.getParts().isEmpty()) {
      body.append("Делови:\n");
      for (OfferPart part : offer.getParts()) {
        body.append("- ").append(part.getName()).append(": ").append(formatMoney(part.getPrice())).append("\n");
      }
      body.append("\n");
    }
    body.append("Цена на делови: ").append(formatMoney(offer.getPartsCost())).append("\n");
    body.append("Цена на работа: ").append(formatMoney(offer.getLaborCost())).append("\n");
    if (discountAmount(offer).signum() > 0) {
      body.append("Попуст за лојален клиент (").append(formatPercent(discountPercent(offer))).append("): -").append(formatMoney(discountAmount(offer))).append("\n");
    }
    body.append("Вкупно: ").append(formatMoney(amount(offer))).append("\n");
    if (offer.getDescription() != null && !offer.getDescription().isBlank()) {
      body.append("\n").append(offer.getDescription());
    }
    return body.toString();
  }

  private void addPart(Offer offer, OfferPartRequest requestedPart, int position) {
    OfferPart part = new OfferPart();
    part.setOffer(offer);
    part.setName(requestedPart.name().trim());
    part.setPrice(requestedPart.price());
    part.setPosition(position);
    offer.getParts().add(part);
  }

  private void validateParts(List<OfferPartRequest> requestedParts) {
    for (OfferPartRequest requestedPart : requestedParts) {
      if (requestedPart.name() == null || requestedPart.name().isBlank()) {
        throw new IllegalArgumentException("Part name is required");
      }
      rejectNegative(requestedPart.price(), "Part price must not be negative");
    }
  }

  private void deliverOffer(Offer offer) {
    EmailDeliveryResult result = emailService.sendHtml(offer.getCustomer().getEmail(), "Понуда за сервис: " + offer.getTitle(), offerHtmlBody(offer), offerBody(offer));
    if (!result.accepted()) {
      throw new IllegalStateException(result.message());
    }
  }

  private BigDecimal costOrZero(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private BigDecimal costOrFallback(BigDecimal value, BigDecimal fallback) {
    if (value != null) {
      return value;
    }
    return fallback == null ? BigDecimal.ZERO : fallback;
  }

  private void rejectNegative(BigDecimal value, String message) {
    if (value == null || value.signum() < 0) {
      throw new IllegalArgumentException(message);
    }
  }

  private BigDecimal discountAmount(BigDecimal subtotalAmount, BigDecimal discountPercent) {
    if (discountPercent == null || discountPercent.signum() <= 0) {
      return BigDecimal.ZERO.setScale(2);
    }
    return subtotalAmount.multiply(discountPercent).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
  }

  private String offerHtmlBody(Offer offer) {
    Customer customer = offer.getCustomer();
    String issueDate = DATE_FORMAT.format(LocalDate.now());
    String vehicleText = offer.getVehicle() == null ? "-" : escape(offer.getVehicle().getPlateNumber() + " - " + offer.getVehicle().getMake() + " " + offer.getVehicle().getModel());
    return """
        <!doctype html>
        <html lang="mk">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Понуда за сервис</title>
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
                            <div style="font-size:22px;font-weight:700;letter-spacing:0;color:#fffaf0;">CarCare ASMS</div>
                            <div style="font-size:13px;color:#d9cdb8;margin-top:8px;">Авто сервис центар</div>
                          </td>
                          <td align="right" style="vertical-align:top;">
                            <div style="font-size:28px;font-weight:700;color:#f2cf7a;">ПОНУДА</div>
                            <div style="font-size:13px;color:#d9cdb8;margin-top:8px;">Бр. #%s</div>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:28px 38px 18px;">
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                        <tr>
                          <td width="50%%" style="vertical-align:top;padding-right:20px;">
                            <div style="font-size:12px;text-transform:uppercase;color:#7a6c58;font-weight:700;margin-bottom:8px;">Клиент</div>
                            <div style="font-size:16px;font-weight:700;color:#14231f;">%s</div>
                            <div style="font-size:14px;line-height:1.6;color:#5b635f;margin-top:6px;">%s<br>%s</div>
                          </td>
                          <td width="50%%" style="vertical-align:top;">
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="font-size:14px;color:#5b635f;">
                              <tr><td style="padding:4px 0;">Датум:</td><td align="right" style="padding:4px 0;color:#14231f;font-weight:700;">%s</td></tr>
                              <tr><td style="padding:4px 0;">Наслов:</td><td align="right" style="padding:4px 0;color:#14231f;font-weight:700;">%s</td></tr>
                              <tr><td style="padding:4px 0;">Возило:</td><td align="right" style="padding:4px 0;color:#14231f;font-weight:700;">%s</td></tr>
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
                          <th align="left" style="padding:13px 12px;border-bottom:1px solid #d9a520;">Опис</th>
                          <th align="center" style="padding:13px 12px;border-bottom:1px solid #d9a520;">Количина</th>
                          <th align="right" style="padding:13px 12px;border-bottom:1px solid #d9a520;">Ед. цена</th>
                          <th align="right" style="padding:13px 12px;border-bottom:1px solid #d9a520;">Износ</th>
                        </tr>
                        %s
                      </table>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:0 38px 36px;">
                      <div style="border-top:1px solid #e3d8c4;margin-top:14px;margin-bottom:14px;"></div>
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="font-size:15px;color:#26322f;">
                        <tr>
                          <td style="padding:6px 0;font-weight:700;">Цена на работа</td>
                          <td align="right" style="padding:6px 0;">%s</td>
                        </tr>
                        %s
                      </table>
                      <div style="border-top:2px solid #d9a520;margin:14px 0;"></div>
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                        <tr>
                          <td style="font-size:24px;font-weight:700;color:#14231f;">ВКУПНО</td>
                          <td align="right" style="font-size:24px;font-weight:700;color:#14231f;">%s</td>
                        </tr>
                      </table>
                      <div style="font-size:13px;line-height:1.6;color:#6f756f;margin-top:22px;">Оваа понуда е информативна и важи според договорените услови со сервисот.</div>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
          </table>
        </body>
        </html>
        """.formatted(
        offer.getId() == null ? "-" : offer.getId(),
        escape(customer.getFullName()),
        escape(blankToDash(customer.getAddress())),
        escape(customer.getEmail()),
        issueDate,
        escape(offer.getTitle()),
        vehicleText,
        offerPartRows(offer),
        formatMoney(offer.getLaborCost()),
        discountRows(offer),
        formatMoney(amount(offer))
    );
  }

  private String offerPartRows(Offer offer) {
    if (offer.getParts().isEmpty()) {
      return """
          <tr>
            <td colspan="4" style="padding:16px 12px;color:#6f756f;border-bottom:1px solid #efe5d4;">Нема додадени делови.</td>
          </tr>
          """;
    }
    StringBuilder rows = new StringBuilder();
    for (OfferPart part : offer.getParts()) {
      String price = formatMoney(part.getPrice());
      rows.append("""
          <tr>
            <td style="padding:14px 12px;border-bottom:1px solid #efe5d4;color:#26322f;">%s</td>
            <td align="center" style="padding:14px 12px;border-bottom:1px solid #efe5d4;color:#26322f;">1</td>
            <td align="right" style="padding:14px 12px;border-bottom:1px solid #efe5d4;color:#26322f;">%s</td>
            <td align="right" style="padding:14px 12px;border-bottom:1px solid #efe5d4;color:#26322f;">%s</td>
          </tr>
          """.formatted(escape(part.getName()), price, price));
    }
    return rows.toString();
  }

  private String formatMoney(BigDecimal value) {
    NumberFormat format = NumberFormat.getNumberInstance(MK_LOCALE);
    format.setMinimumFractionDigits(2);
    format.setMaximumFractionDigits(2);
    return format.format(value) + " ден.";
  }

  private String formatPercent(BigDecimal value) {
    NumberFormat format = NumberFormat.getNumberInstance(MK_LOCALE);
    format.setMinimumFractionDigits(0);
    format.setMaximumFractionDigits(2);
    return format.format(value) + "%";
  }

  private String discountRows(Offer offer) {
    if (discountAmount(offer).signum() <= 0) {
      return "";
    }
    return """
        <tr>
          <td style="padding:6px 0;font-weight:700;color:#166534;">Попуст за лојален клиент (%s)</td>
          <td align="right" style="padding:6px 0;color:#166534;">-%s</td>
        </tr>
        """.formatted(formatPercent(discountPercent(offer)), formatMoney(discountAmount(offer)));
  }

  private BigDecimal subtotalAmount(Offer offer) {
    return offer.getSubtotalAmount() == null ? offer.getPartsCost().add(offer.getLaborCost()) : offer.getSubtotalAmount();
  }

  private BigDecimal discountPercent(Offer offer) {
    return offer.getDiscountPercent() == null ? BigDecimal.ZERO.setScale(2) : offer.getDiscountPercent();
  }

  private BigDecimal discountAmount(Offer offer) {
    return offer.getDiscountAmount() == null ? discountAmount(subtotalAmount(offer), discountPercent(offer)) : offer.getDiscountAmount();
  }

  private BigDecimal amount(Offer offer) {
    return offer.getAmount() == null ? subtotalAmount(offer).subtract(discountAmount(offer)) : offer.getAmount();
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
