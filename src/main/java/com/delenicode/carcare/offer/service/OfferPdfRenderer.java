package com.delenicode.carcare.offer.service;

import com.delenicode.carcare.notification.PdfService;
import com.delenicode.carcare.offer.model.Offer;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfferPdfRenderer {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
  private static final Locale MK_LOCALE = Locale.forLanguageTag("mk-MK");

  private final PdfService pdfService;
  private final OfferPricingService pricing;

  public byte[] render(Offer offer) {
    List<String[]> parts = offer.getParts().stream()
        .map(p -> new String[]{p.getName(), formatMoney(p.getPrice())})
        .toList();

    boolean hasDiscount = pricing.discountAmount(offer).signum() > 0;
    String discountLabel = "Попуст за лојален клиент (" + formatPercent(pricing.discountPercent(offer)) + ")";

    return pdfService.renderOfferReport(
        String.valueOf(offer.getId()),
        offer.getTitle(),
        offer.getDescription(),
        DATE_FORMAT.format(LocalDate.now()),
        offer.getExpiresOn() == null ? null : DATE_FORMAT.format(offer.getExpiresOn()),
        offer.getCustomer().getFullName(),
        blankToDash(offer.getCustomer().getAddress()),
        offer.getCustomer().getEmail(),
        vehicleText(offer),
        parts,
        formatMoney(offer.getPartsCost()),
        formatMoney(offer.getLaborCost()),
        hasDiscount,
        discountLabel,
        formatMoney(pricing.discountAmount(offer)),
        formatMoney(pricing.subtotalAmount(offer)),
        formatMoney(pricing.amount(offer)));
  }

  private String vehicleText(Offer offer) {
    return offer.getVehicle() == null
        ? "-"
        : offer.getVehicle().getPlateNumber() + " — " + offer.getVehicle().getMake() + " " + offer.getVehicle().getModel();
  }

  private String formatMoney(BigDecimal value) {
    NumberFormat format = NumberFormat.getNumberInstance(MK_LOCALE);
    format.setMinimumFractionDigits(2);
    format.setMaximumFractionDigits(2);
    return format.format(pricing.money(value)) + " ден.";
  }

  private String formatPercent(BigDecimal value) {
    NumberFormat format = NumberFormat.getNumberInstance(MK_LOCALE);
    format.setMinimumFractionDigits(0);
    format.setMaximumFractionDigits(2);
    return format.format(value) + "%";
  }

  private String blankToDash(String value) {
    return value == null || value.isBlank() ? "-" : value;
  }
}
