package com.delenicode.carcare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.delenicode.carcare.notification.EmailService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {
  @Mock
  JavaMailSender mailSender;

  EmailService emailService;

  @BeforeEach
  void setUp() {
    emailService = new EmailService(mailSender);
    ReflectionTestUtils.setField(emailService, "fromAddress", "no-reply@carcare.local");
  }

  @Test
  void sendHtmlUsesMultipartAlternativeMessage() {
    MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
    when(mailSender.createMimeMessage()).thenReturn(message);

    var result = emailService.sendHtml("client@carcare.test", "Понуда", "<strong>Понуда</strong>", "Понуда");

    assertThat(result.accepted()).isTrue();
    verify(mailSender).send(message);
  }
}
