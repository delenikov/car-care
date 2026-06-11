package com.delenicode.carcare.user;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
  private final AppUserRepository users;
  private final RoleRepository roles;
  private final PasswordEncoder passwordEncoder;

  @Transactional(readOnly = true)
  public List<UserResponse> findAll() {
    return users.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional
  public UserResponse create(UserRequest request) {
    if (users.existsByEmail(request.email())) {
      throw new IllegalArgumentException("User email already exists");
    }
    AppUser user = new AppUser();
    user.setEmail(request.email());
    user.setFullName(request.fullName());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    Set<String> names = request.roles() == null || request.roles().isEmpty() ? Set.of("ROLE_EMPLOYEE") : request.roles();
    user.setRoles(names.stream().map(this::roleByName).collect(Collectors.toSet()));
    return toResponse(users.save(user));
  }

  @Transactional
  public UserResponse ensureAdminUser(String email, String fullName, String password) {
    AppUser user = users.findByEmail(email).orElse(null);
    Role adminRole = roleByName("ROLE_ADMIN");
    if (user == null) {
      AppUser admin = new AppUser();
      admin.setEmail(email);
      admin.setFullName(fullName);
      admin.setPasswordHash(passwordEncoder.encode(password));
      admin.getRoles().add(adminRole);
      return toResponse(users.save(admin));
    }

    user.setFullName(fullName);
    user.setEnabled(true);
    user.getRoles().add(adminRole);
    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      user.setPasswordHash(passwordEncoder.encode(password));
    }
    return toResponse(users.save(user));
  }

  private Role roleByName(String name) {
    return roles.findByName(name).orElseThrow(() -> new IllegalArgumentException("Unknown role: " + name));
  }

  public UserResponse toResponse(AppUser user) {
    Set<String> names = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
    return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.isEnabled(), user.getFailedLoginAttempts(), names);
  }
}
