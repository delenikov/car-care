package com.delenicode.carcare.document.service;


import com.delenicode.carcare.document.model.ServiceDocument;
import com.delenicode.carcare.document.model.ServiceDocumentView;
import com.delenicode.carcare.customer.model.Customer;
import com.delenicode.carcare.servicerecord.model.ServiceRecord;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ServiceDocumentViewFactory {
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
  private static final Locale MK_LOCALE = Locale.forLanguageTag("mk-MK");

  public ServiceDocumentView create(ServiceDocument document) {
    Customer customer = document.getCustomer();
    ServiceRecord record = document.getServiceRecord();
    if (record == null) {
      return new ServiceDocumentView(
          String.valueOf(document.getId()),
          customer.getFullName(),
          blankToDash(customer.getAddress()),
          customer.getEmail(),
          "-",
          "-",
          "-",
          "Сервисен документ",
          "-",
          "-",
          formatMoney(BigDecimal.ZERO),
          formatMoney(BigDecimal.ZERO),
          formatMoney(BigDecimal.ZERO),
          document.getFileName(),
          document.getContentType(),
          false);
    }
    return new ServiceDocumentView(
        String.valueOf(record.getId()),
        customer.getFullName(),
        blankToDash(customer.getAddress()),
        customer.getEmail(),
        DATE_FORMAT.format(record.getServiceDate()),
        vehicleText(record),
        record.getOdometer() == null ? "-" : NumberFormat.getIntegerInstance(MK_LOCALE).format(record.getOdometer()),
        record.getServiceType(),
        blankToDash(record.getReplacedParts()),
        blankToDash(record.getNotes()),
        formatMoney(record.getPartsCost()),
        formatMoney(record.getLaborCost()),
        formatMoney(record.getTotalAmount()),
        document.getFileName(),
        document.getContentType(),
        true);
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
}
