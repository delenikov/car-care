package com.delenicode.carcare.document.model;

public record ServiceDocumentView(
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
    String total,
    String fileName,
    String contentType,
    boolean serviceRecordDocument
) {
}
