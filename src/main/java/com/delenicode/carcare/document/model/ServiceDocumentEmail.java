package com.delenicode.carcare.document.model;

public record ServiceDocumentEmail(
    String subject,
    String htmlBody,
    String textBody
) {
}
