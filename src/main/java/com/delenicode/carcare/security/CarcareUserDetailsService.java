package com.delenicode.carcare.security;

import com.delenicode.carcare.user.AppUser;
import com.delenicode.carcare.user.AppUserRepository;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CarcareUserDetailsService implements UserDetailsService {

  private final AppUserRepository users;

  @Override
  public UserDetails loadUserByUsername(String username) {
    AppUser user = users.findByEmail(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
    return new SecurityUser(user);
  }

  public record SecurityUser(AppUser user) implements UserDetails {
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
      return user.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.getName())).toList();
    }

    @Override
    public String getPassword() {
      return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
      return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
      return true;
    }

    @Override
    public boolean isAccountNonLocked() {
      return user.getLockedUntil() == null || user.getLockedUntil().isBefore(java.time.Instant.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
      return true;
    }

    @Override
    public boolean isEnabled() {
      return user.isEnabled();
    }
  }
}
