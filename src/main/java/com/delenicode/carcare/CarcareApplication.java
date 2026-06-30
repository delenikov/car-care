package com.delenicode.carcare;

import com.delenicode.carcare.user.service.UserService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CarcareApplication {

  public static void main(String[] args) {
    SpringApplication.run(CarcareApplication.class, args);
  }

  @Bean
  ApplicationRunner adminUserInitializer(UserService users) {
    return args -> users.ensureAdminUser("admin@carcare.local", "System Admin", "admin123");
  }

}
