package com.delenicode.carcare.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import com.delenicode.carcare.common.LogSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService implements HtmlEmailSender, HtmlAttachmentEmailSender {

  private final JavaMailSender mailSender;

  @Value("${app.mail.from:no-reply@carcare.local}")
  private String fromAddress;

  public EmailDeliveryResult send(String recipient, String subject, String body) {
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(fromAddress);
      message.setTo(recipient);
      message.setSubject(subject);
      message.setText(body);
      mailSender.send(message);
      log.info("Email sent. Type: text. Recipient: {}. Subject: {}", LogSanitizer.email(recipient), subject);
      return new EmailDeliveryResult(recipient, subject, true, "Email sent");
    } catch (MailException ex) {
      log.warn("Email failed. Type: text. Recipient: {}. Subject: {}. Error: {}", LogSanitizer.email(recipient), subject, ex.getMessage());
      return new EmailDeliveryResult(recipient, subject, false, ex.getMessage());
    }
  }

  public EmailDeliveryResult sendHtml(String recipient, String subject, String htmlBody, String textBody) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(fromAddress);
      helper.setTo(recipient);
      helper.setSubject(subject);
      helper.setText(textBody, htmlBody);

      long start = System.currentTimeMillis();
      log.info("Before send");
      mailSender.send(message);
      long end = System.currentTimeMillis() - start;
      log.info("After send: {} ms", end);

      log.info("Email sent. Type: HTML. Recipient: {}. Subject: {}", LogSanitizer.email(recipient), subject);
      return new EmailDeliveryResult(recipient, subject, true, "Email sent");
    } catch (MessagingException | MailException ex) {
      log.warn("Email failed. Type: HTML. Recipient: {}. Subject: {}. Error: {}", LogSanitizer.email(recipient), subject, ex.getMessage());
      return new EmailDeliveryResult(recipient, subject, false, ex.getMessage());
    }
  }

  public EmailDeliveryResult sendHtmlWithAttachment(String recipient, String subject, String htmlBody, String textBody, String attachmentName, String contentType, byte[] attachment) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(fromAddress);
      helper.setTo(recipient);
      helper.setSubject(subject);
      helper.setText(textBody, htmlBody);
      helper.addAttachment(attachmentName, new ByteArrayResource(attachment), contentType);

      long start = System.currentTimeMillis();
      log.info("Before send");
      mailSender.send(message);
      long end = System.currentTimeMillis() - start;
      log.info("After send: {} ms", end);

      log.info("Email sent. Type: HTML with attachment. Recipient: {}. Subject: {}. Attachment: {}. Content type: {}", LogSanitizer.email(recipient), subject, attachmentName, contentType);
      return new EmailDeliveryResult(recipient, subject, true, "Email sent");
    } catch (MessagingException | MailException ex) {
      log.warn("Email failed. Type: HTML with attachment. Recipient: {}. Subject: {}. Attachment: {}. Error: {}", LogSanitizer.email(recipient), subject, attachmentName, ex.getMessage());
      return new EmailDeliveryResult(recipient, subject, false, ex.getMessage());
    }
  }
}
