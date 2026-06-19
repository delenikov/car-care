package com.delenicode.carcare.notification;

public interface HtmlAttachmentEmailSender {
  EmailDeliveryResult sendHtmlWithAttachment(
      String recipient,
      String subject,
      String htmlBody,
      String textBody,
      String attachmentName,
      String contentType,
      byte[] attachment);
}
