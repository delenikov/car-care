package com.delenicode.carcare;

import static org.assertj.core.api.Assertions.assertThat;

import com.delenicode.carcare.config.TimeZoneConfig;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class TimeZoneConfigTest {
  private TimeZone previousTimeZone;
  private String previousUserTimeZone;

  @BeforeEach
  void setUp() {
    previousTimeZone = TimeZone.getDefault();
    previousUserTimeZone = System.getProperty("user.timezone");
  }

  @AfterEach
  void tearDown() {
    TimeZone.setDefault(previousTimeZone);
    if (previousUserTimeZone == null) {
      System.clearProperty("user.timezone");
    } else {
      System.setProperty("user.timezone", previousUserTimeZone);
    }
  }

  @Test
  void appliesConfiguredApplicationTimeZoneAsJvmDefault() {
    TimeZoneConfig config = new TimeZoneConfig();
    config.setEnvironment(new MockEnvironment().withProperty("app.time-zone", "Europe/Skopje"));

    config.postProcessBeanFactory(null);

    assertThat(TimeZone.getDefault().getID()).isEqualTo("Europe/Skopje");
    assertThat(System.getProperty("user.timezone")).isEqualTo("Europe/Skopje");
  }
}
