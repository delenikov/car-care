package com.delenicode.carcare.offer.service;


import com.delenicode.carcare.offer.model.Offer;
import com.delenicode.carcare.offer.model.OfferEmail;
import com.delenicode.carcare.offer.model.OfferPart;
import com.delenicode.carcare.vehicle.model.Vehicle;
import com.delenicode.carcare.customer.model.Customer;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class OfferEmailRenderer {
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
  private static final Locale MK_LOCALE = Locale.forLanguageTag("mk-MK");

  private final TemplateEngine templateEngine;
  private final OfferPricingService pricing;

  public OfferEmail render(Offer offer) {
    return new OfferEmail(subject(offer), renderHtml(offer), renderText(offer));
  }

  public String renderHtml(Offer offer) {
    Context context = new Context(MK_LOCALE);
    Customer customer = offer.getCustomer();
    context.setVariable("offerId", offer.getId() == null ? "-" : offer.getId());
    context.setVariable("title", offer.getTitle());
    context.setVariable("description", blankToDash(offer.getDescription()));
    context.setVariable("issueDate", DATE_FORMAT.format(LocalDate.now()));
    context.setVariable("customerName", customer.getFullName());
    context.setVariable("customerAddress", blankToDash(customer.getAddress()));
    context.setVariable("customerEmail", customer.getEmail());
    context.setVariable("vehicle", vehicleText(offer));
    context.setVariable("parts", offer.getParts().stream()
        .map(part -> Map.of("name", part.getName(), "price", formatMoney(part.getPrice())))
        .toList());
    context.setVariable("partsCost", formatMoney(offer.getPartsCost()));
    context.setVariable("laborCost", formatMoney(offer.getLaborCost()));
    context.setVariable("discountPercent", formatPercent(pricing.discountPercent(offer)));
    context.setVariable("discountAmount", formatMoney(pricing.discountAmount(offer)));
    context.setVariable("hasDiscount", pricing.discountAmount(offer).signum() > 0);
    context.setVariable("total", formatMoney(pricing.amount(offer)));
    return templateEngine.process("offer-email", context);
  }

  public String renderText(Offer offer) {
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
    if (pricing.discountAmount(offer).signum() > 0) {
      body.append("Попуст за лојален клиент (")
          .append(formatPercent(pricing.discountPercent(offer)))
          .append("): -")
          .append(formatMoney(pricing.discountAmount(offer)))
          .append("\n");
    }
    body.append("Вкупно: ").append(formatMoney(pricing.amount(offer))).append("\n");
    if (offer.getDescription() != null && !offer.getDescription().isBlank()) {
      body.append("\n").append(offer.getDescription());
    }
    return body.toString();
  }

  private String subject(Offer offer) {
    return "Понуда за сервис: " + offer.getTitle();
  }

  private String vehicleText(Offer offer) {
    return offer.getVehicle() == null
        ? "-"
        : offer.getVehicle().getPlateNumber() + " - " + offer.getVehicle().getMake() + " " + offer.getVehicle().getModel();
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
