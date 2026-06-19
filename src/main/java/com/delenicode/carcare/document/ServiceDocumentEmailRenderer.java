package com.delenicode.carcare.document;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class ServiceDocumentEmailRenderer {
  private static final Locale MK_LOCALE = Locale.forLanguageTag("mk-MK");

  private final TemplateEngine templateEngine;
  private final ServiceDocumentViewFactory views;

  public ServiceDocumentEmail render(ServiceDocument document) {
    ServiceDocumentView view = views.create(document);
    return new ServiceDocumentEmail(subject(view), renderHtml(view), renderText(view));
  }

  String renderHtml(ServiceDocumentView view) {
    Context context = new Context(MK_LOCALE);
    context.setVariable("documentNumber", view.documentNumber());
    context.setVariable("customerName", view.customerName());
    context.setVariable("customerAddress", view.customerAddress());
    context.setVariable("customerEmail", view.customerEmail());
    context.setVariable("serviceDate", view.serviceDate());
    context.setVariable("vehicle", view.vehicle());
    context.setVariable("odometer", view.odometer());
    context.setVariable("serviceType", view.serviceType());
    context.setVariable("replacedParts", view.replacedParts());
    context.setVariable("notes", view.notes());
    context.setVariable("partsCost", view.partsCost());
    context.setVariable("laborCost", view.laborCost());
    context.setVariable("total", view.total());
    context.setVariable("serviceRecordDocument", view.serviceRecordDocument());
    return templateEngine.process("service-document-email", context);
  }

  String renderText(ServiceDocumentView view) {
    if (!view.serviceRecordDocument()) {
      return "Документ за " + view.customerName();
    }
    return """
        Клиент: %s
        Возило: %s
        Датум на сервис: %s
        Тип на сервис: %s
        Километража: %s km
        Заменети делови: %s
        Цена на делови: %s
        Цена на работа: %s
        Вкупно: %s
        Белешки: %s
        """.formatted(
        view.customerName(),
        view.vehicle(),
        view.serviceDate(),
        view.serviceType(),
        view.odometer(),
        view.replacedParts(),
        view.partsCost(),
        view.laborCost(),
        view.total(),
        view.notes());
  }

  private String subject(ServiceDocumentView view) {
    return view.serviceRecordDocument()
        ? "Извештај за завршен сервис: " + view.serviceType()
        : "Сервисен документ";
  }
}
