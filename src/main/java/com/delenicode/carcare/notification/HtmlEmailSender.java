package com.delenicode.carcare.notification;

public interface HtmlEmailSender {
  EmailDeliveryResult sendHtml(String recipient, String subject, String htmlBody, String textBody);
}
