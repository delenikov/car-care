package com.delenicode.carcare.user.service;


import com.delenicode.carcare.user.dto.request.UserRequest;
import com.delenicode.carcare.user.dto.request.UserUpdateRequest;
import com.delenicode.carcare.user.dto.response.UserResponse;
import com.delenicode.carcare.common.LogSanitizer;
import com.delenicode.carcare.user.model.AppUser;
import com.delenicode.carcare.user.model.Role;
import com.delenicode.carcare.user.repository.AppUserRepository;
import com.delenicode.carcare.user.repository.RoleRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
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
    AppUser saved = users.save(user);
    log.info("User created. User ID: {}. Email: {}. Role count: {}", saved.getId(), LogSanitizer.email(saved.getEmail()), saved.getRoles().size());
    return toResponse(saved);
  }

  @Transactional
  public UserResponse update(Long id, UserUpdateRequest request) {
    AppUser user = users.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
    AppUser existing = users.findByEmail(request.email()).orElse(null);
    if (existing != null && !existing.getId().equals(id)) {
      throw new IllegalArgumentException("User email already exists");
    }

    user.setEmail(request.email());
    user.setFullName(request.fullName());
    user.setEnabled(request.enabled() == null || request.enabled());
    if (request.password() != null && !request.password().isBlank()) {
      user.setPasswordHash(passwordEncoder.encode(request.password()));
    }
    if (request.roles() != null && !request.roles().isEmpty()) {
      user.setRoles(request.roles().stream().map(this::roleByName).collect(Collectors.toSet()));
    }
    AppUser saved = users.save(user);
    log.info("User updated. User ID: {}. Email: {}. Enabled: {}. Role count: {}", saved.getId(), LogSanitizer.email(saved.getEmail()), saved.isEnabled(), saved.getRoles().size());
    return toResponse(saved);
  }

  @Transactional
  public void delete(Long id) {
    AppUser user = users.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
    user.setEnabled(false);
    users.save(user);
    log.info("User disabled. User ID: {}", user.getId());
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
      AppUser saved = users.save(admin);
      log.info("Admin user ensured. Action: created. User ID: {}. Email: {}", saved.getId(), LogSanitizer.email(saved.getEmail()));
      return toResponse(saved);
    }

    user.setFullName(fullName);
    user.setEnabled(true);
    user.getRoles().add(adminRole);
    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      user.setPasswordHash(passwordEncoder.encode(password));
    }
    AppUser saved = users.save(user);
    log.info("Admin user ensured. Action: updated. User ID: {}. Email: {}", saved.getId(), LogSanitizer.email(saved.getEmail()));
    return toResponse(saved);
  }

  private Role roleByName(String name) {
    return roles.findByName(name).orElseThrow(() -> new IllegalArgumentException("Unknown role: " + name));
  }

  public UserResponse toResponse(AppUser user) {
    Set<String> names = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
    return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.isEnabled(), user.getFailedLoginAttempts(), names);
  }
}
