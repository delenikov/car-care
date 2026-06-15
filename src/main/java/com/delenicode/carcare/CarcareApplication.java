package com.delenicode.carcare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CarcareApplication {

  public static void main(String[] args) {
    SpringApplication.run(CarcareApplication.class, args);
  }

//  @Bean
//  ApplicationRunner adminUserInitializer(UserService users) {
//    return args -> users.ensureAdminUser("admin@carcare.local", "System Admin", "admin123");
//  }

}
