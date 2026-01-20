package com.bookcover.archive.config.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * 보안 관련 유틸리티 클래스.
 * 현재 인증된 사용자 정보를 조회하는 메서드를 제공합니다.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Utility class
    }

    /**
     * 현재 인증된 사용자의 ID를 반환합니다.
     */
    public static Optional<UUID> getCurrentUserId() {
        return getCurrentUserDetails().map(CustomUserDetails::getId);
    }

    /**
     * 현재 인증된 사용자의 이메일을 반환합니다.
     */
    public static Optional<String> getCurrentUserEmail() {
        return getCurrentUserDetails().map(CustomUserDetails::getUsername);
    }

    /**
     * 현재 인증된 사용자의 UserDetails를 반환합니다.
     */
    public static Optional<CustomUserDetails> getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CustomUserDetails) {
            return Optional.of((CustomUserDetails) authentication.getPrincipal());
        }
        return Optional.empty();
    }

    /**
     * 현재 사용자가 인증되었는지 확인합니다.
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CustomUserDetails;
    }

    /**
     * 현재 사용자가 특정 역할을 가지고 있는지 확인합니다.
     */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(roleWithPrefix));
    }
}
