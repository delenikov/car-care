package com.delenicode.carcare;


import com.delenicode.carcare.user.model.Employee;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.delenicode.carcare.user.model.AppUser;
import com.delenicode.carcare.user.repository.AppUserRepository;
import com.delenicode.carcare.user.model.Role;
import com.delenicode.carcare.user.repository.RoleRepository;
import com.delenicode.carcare.user.dto.request.UserRequest;
import com.delenicode.carcare.user.service.UserService;
import com.delenicode.carcare.user.dto.request.UserUpdateRequest;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
  @Mock
  AppUserRepository users;
  @Mock
  RoleRepository roles;
  @Mock
  PasswordEncoder passwordEncoder;

  UserService userService;

  @BeforeEach
  void setUp() {
    userService = new UserService(users, roles, passwordEncoder);
  }

  @Test
  void createAssignsEmployeeRoleWhenRolesAreMissing() {
    Role employee = new Role("ROLE_EMPLOYEE");
    when(users.existsByEmail("tech@carcare.test")).thenReturn(false);
    when(roles.findByName("ROLE_EMPLOYEE")).thenReturn(Optional.of(employee));
    when(passwordEncoder.encode("password123")).thenReturn("hashed");
    when(users.save(org.mockito.ArgumentMatchers.any(AppUser.class))).thenAnswer(invocation -> {
      AppUser user = invocation.getArgument(0);
      user.setId(10L);
      return user;
    });

    var response = userService.create(new UserRequest("tech@carcare.test", "Technician", "password123", Set.of()));

    assertThat(response.email()).isEqualTo("tech@carcare.test");
    assertThat(response.roles()).containsExactly("ROLE_EMPLOYEE");
  }

  @Test
  void updateChangesProfileRolesEnabledStatusAndPassword() {
    AppUser user = user(10L, "old@carcare.test");
    Role manager = new Role("ROLE_MANAGER");
    when(users.findById(10L)).thenReturn(Optional.of(user));
    when(users.findByEmail("new@carcare.test")).thenReturn(Optional.empty());
    when(roles.findByName("ROLE_MANAGER")).thenReturn(Optional.of(manager));
    when(passwordEncoder.encode("password456")).thenReturn("new-hash");
    when(users.save(user)).thenReturn(user);

    var response = userService.update(10L, new UserUpdateRequest("new@carcare.test", "Updated Manager", false, "password456", Set.of("ROLE_MANAGER")));

    assertThat(response.email()).isEqualTo("new@carcare.test");
    assertThat(response.fullName()).isEqualTo("Updated Manager");
    assertThat(response.enabled()).isFalse();
    assertThat(response.roles()).containsExactly("ROLE_MANAGER");
    assertThat(user.getPasswordHash()).isEqualTo("new-hash");
  }

  @Test
  void updateRejectsDuplicateEmailOwnedByAnotherUser() {
    AppUser user = user(10L, "old@carcare.test");
    AppUser other = user(11L, "new@carcare.test");
    when(users.findById(10L)).thenReturn(Optional.of(user));
    when(users.findByEmail("new@carcare.test")).thenReturn(Optional.of(other));

    assertThatThrownBy(() -> userService.update(10L, new UserUpdateRequest("new@carcare.test", "Updated Manager", true, null, Set.of("ROLE_MANAGER"))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("User email already exists");
  }

  @Test
  void deleteDisablesUserAccount() {
    AppUser user = user(10L, "tech@carcare.test");
    when(users.findById(10L)).thenReturn(Optional.of(user));

    userService.delete(10L);

    assertThat(user.isEnabled()).isFalse();
    verify(users).save(user);
  }

  private AppUser user(Long id, String email) {
    AppUser user = new AppUser();
    user.setId(id);
    user.setEmail(email);
    user.setFullName("Test User");
    user.setPasswordHash("old-hash");
    user.setEnabled(true);
    return user;
  }
}
