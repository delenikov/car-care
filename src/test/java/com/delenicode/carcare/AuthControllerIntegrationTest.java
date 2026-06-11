//package com.delenicode.carcare;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//import com.delenicode.carcare.user.AppUser;
//import com.delenicode.carcare.user.AppUserRepository;
//import com.delenicode.carcare.user.Role;
//import com.delenicode.carcare.user.RoleRepository;
//import java.util.Set;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.web.servlet.MockMvc;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//@ActiveProfiles("test")
//class AuthControllerIntegrationTest {
//  @Autowired
//  MockMvc mockMvc;
//
//  @Autowired
//  AppUserRepository users;
//
//  @Autowired
//  RoleRepository roles;
//
//  @Autowired
//  PasswordEncoder passwordEncoder;
//
//  @BeforeEach
//  void setUp() {
//    Role role = roles.findByName("ROLE_EMPLOYEE").orElseGet(() -> roles.save(new Role("ROLE_EMPLOYEE")));
//    users.findByEmail("tech@carcare.local").orElseGet(() -> {
//      AppUser user = new AppUser();
//      user.setEmail("tech@carcare.local");
//      user.setFullName("Test Technician");
//      user.setPasswordHash(passwordEncoder.encode("password123"));
//      user.setRoles(Set.of(role));
//      return users.save(user);
//    });
//  }
//
//  @Test
//  void loginReturnsWrappedAccessAndRefreshTokens() throws Exception {
//    mockMvc.perform(post("/api/auth/login")
//            .contentType(MediaType.APPLICATION_JSON)
//            .content("{\"email\":\"tech@carcare.local\",\"password\":\"password123\"}"))
//        .andExpect(status().isOk())
//        .andExpect(jsonPath("$.success").value(true))
//        .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
//        .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
//        .andExpect(jsonPath("$.data.user.email").value("tech@carcare.local"));
//  }
//
//  @Test
//  void loginRejectsBadPassword() throws Exception {
//    mockMvc.perform(post("/api/auth/login")
//            .contentType(MediaType.APPLICATION_JSON)
//            .content("{\"email\":\"tech@carcare.local\",\"password\":\"wrong-password\"}"))
//        .andExpect(status().isUnauthorized())
//        .andExpect(jsonPath("$.success").value(false));
//  }
//}
