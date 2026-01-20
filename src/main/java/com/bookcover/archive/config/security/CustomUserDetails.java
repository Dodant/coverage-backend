package com.bookcover.archive.config.security;

import com.bookcover.archive.domain.user.User;
import com.bookcover.archive.domain.user.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spring Security UserDetails 구현체.
 * User 엔티티를 Spring Security에서 사용할 수 있도록 래핑합니다.
 */
public class CustomUserDetails implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final String displayName;
    private final UserStatus status;
    private final Instant suspendedUntil;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.displayName = user.getDisplayName();
        this.status = user.getStatus();
        this.suspendedUntil = user.getSuspendedUntil();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    public UUID getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
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
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        if (status == UserStatus.SUSPENDED) {
            // 정지 기간이 지났으면 잠금 해제
            return suspendedUntil != null && Instant.now().isAfter(suspendedUntil);
        }
        return status != UserStatus.DELETED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
