package com.delenicode.carcare.document;

public record ServiceDocumentEmail(
    String subject,
    String htmlBody,
    String textBody
) {
}
