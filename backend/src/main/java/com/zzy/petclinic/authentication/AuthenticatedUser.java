package com.zzy.petclinic.authentication;

import com.zzy.petclinic.authorization.UserAuthorities;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** 把项目用户适配成 Spring Security 认识的 {@link UserDetails}。 */
public final class AuthenticatedUser implements UserDetails {
  private final Long id;
  private final String username;
  private final String password;
  private final String displayName;
  private final String phone;
  private final String email;
  private final String accountType;
  private final String status;
  private final int tokenVersion;
  private final List<GrantedAuthority> authorities;

  public AuthenticatedUser(SysUser user, UserAuthorities userAuthorities) {
    this.id = user.getId();
    this.username = user.getUsername();
    this.password = user.getPasswordHash();
    this.displayName = user.getDisplayName();
    this.phone = user.getPhone();
    this.email = user.getEmail();
    this.accountType = user.getAccountType();
    this.status = user.getStatus();
    this.tokenVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
    this.authorities =
        userAuthorities.authorityCodes().stream()
            .map(code -> (GrantedAuthority) new SimpleGrantedAuthority(code))
            .toList();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isEnabled() {
    return "ACTIVE".equals(status);
  }

  public Long getId() {
    return id;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getPhone() {
    return phone;
  }

  public String getEmail() {
    return email;
  }

  public String getAccountType() {
    return accountType;
  }

  public int getTokenVersion() {
    return tokenVersion;
  }
}
