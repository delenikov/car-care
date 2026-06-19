package com.delenicode.carcare;

import static org.assertj.core.api.Assertions.assertThat;

import com.delenicode.carcare.customer.Customer;
import com.delenicode.carcare.offer.Offer;
import com.delenicode.carcare.offer.OfferEmail;
import com.delenicode.carcare.offer.OfferEmailRenderer;
import com.delenicode.carcare.offer.OfferPart;
import com.delenicode.carcare.offer.OfferPricingService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class OfferEmailRendererTest {
  @Test
  void rendersMacedonianHtmlAndPlainTextOfferEmail() {
    OfferEmailRenderer renderer = new OfferEmailRenderer(templateEngine(), new OfferPricingService());

    OfferEmail email = renderer.render(offer());

    assertThat(email.subject()).isEqualTo("Понуда за сервис: Brake inspection");
    assertThat(email.htmlBody()).contains("ПОНУДА", "Попуст за лојален клиент", "1.800,00 ден.");
    assertThat(email.textBody()).contains("Понуда: Brake inspection", "Цена на делови: 1.200,00 ден.", "Вкупно: 1.800,00 ден.");
  }

  private TemplateEngine templateEngine() {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    return engine;
  }

  private Offer offer() {
    Customer customer = new Customer();
    customer.setId(10L);
    customer.setFullName("Ada Lovelace");
    customer.setAddress("Test Street 1");
    customer.setEmail("ada@carcare.test");

    Offer offer = new Offer();
    offer.setId(20L);
    offer.setCustomer(customer);
    offer.setTitle("Brake inspection");
    offer.setDescription("Inspect brakes");
    offer.setPartsCost(new BigDecimal("1200.00"));
    offer.setLaborCost(new BigDecimal("800.00"));
    offer.setSubtotalAmount(new BigDecimal("2000.00"));
    offer.setDiscountPercent(new BigDecimal("10.00"));
    offer.setDiscountAmount(new BigDecimal("200.00"));
    offer.setAmount(new BigDecimal("1800.00"));

    OfferPart part = new OfferPart();
    part.setOffer(offer);
    part.setName("Brake pads");
    part.setPrice(new BigDecimal("1200.00"));
    part.setPosition(0);
    offer.getParts().add(part);
    return offer;
  }
}
