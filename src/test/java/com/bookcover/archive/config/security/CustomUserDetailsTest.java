package com.bookcover.archive.config.security;

import com.bookcover.archive.domain.user.User;
import com.bookcover.archive.domain.user.UserRole;
import com.bookcover.archive.domain.user.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 로그인(DaoAuthenticationProvider)이 보는 계정 상태 판정. 정지 만료 계정이 로그인으로 복구될 수 있어야 한다. */
class CustomUserDetailsTest {

    private static CustomUserDetails details(UserStatus status, Instant suspendedUntil) {
        User user = new User();
        user.setEmail("a@b.c");
        user.setPasswordHash("x");
        user.setRole(UserRole.MEMBER);
        user.setStatus(status);
        user.setSuspendedUntil(suspendedUntil);
        return new CustomUserDetails(user);
    }

    @Test
    void 정지_기간이_지나면_로그인할_수_있다() {
        CustomUserDetails d = details(UserStatus.SUSPENDED, Instant.now().minus(1, ChronoUnit.MINUTES));
        assertTrue(d.isEnabled() && d.isAccountNonLocked());
    }

    @Test
    void 정지_중이면_잠겨_있다() {
        assertFalse(details(UserStatus.SUSPENDED, Instant.now().plus(1, ChronoUnit.HOURS)).isAccountNonLocked());
    }

    @Test
    void 탈퇴_계정은_비활성이다() {
        assertFalse(details(UserStatus.DELETED, null).isEnabled());
    }
}
