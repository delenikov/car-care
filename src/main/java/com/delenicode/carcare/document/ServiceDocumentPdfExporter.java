package com.delenicode.carcare.document;

import com.delenicode.carcare.notification.PdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ServiceDocumentPdfExporter {
  private final PdfService pdfService;
  private final ServiceDocumentViewFactory views;

  public byte[] export(ServiceDocument document) {
    ServiceDocumentView view = views.create(document);
    if (!view.serviceRecordDocument()) {
      return pdfService.renderServiceSummary(view.fileName(), "Документ за: " + view.customerName());
    }
    return pdfService.renderServiceReport(
        view.documentNumber(),
        view.customerName(),
        view.customerAddress(),
        view.customerEmail(),
        view.serviceDate(),
        view.vehicle(),
        view.odometer(),
        view.serviceType(),
        view.replacedParts(),
        view.notes(),
        view.partsCost(),
        view.laborCost(),
        view.total());
  }
}
