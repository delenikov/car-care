package com.delenicode.carcare.config;

import java.util.TimeZone;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

@Slf4j
@Configuration
public class TimeZoneConfig implements BeanFactoryPostProcessor, EnvironmentAware, Ordered {

  private Environment environment;

  @Override
  public void setEnvironment(@NonNull Environment environment) {
    this.environment = environment;
  }

  @Override
  public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
    final String DEFAULT_TIME_ZONE = "Europe/Skopje";
    String timeZone = environment.getProperty("app.time-zone", DEFAULT_TIME_ZONE);
    System.setProperty("user.timezone", timeZone);
    TimeZone.setDefault(TimeZone.getTimeZone(timeZone));
    log.info("Configured JVM default timezone to {}. Effective timezone is {}.", timeZone, TimeZone.getDefault().getID());
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
