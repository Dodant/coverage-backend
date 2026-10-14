package com.bookcover.archive.service.impl;

import com.bookcover.archive.controller.dto.user.ChangePasswordRequest;
import com.bookcover.archive.controller.dto.user.UpdateProfileRequest;
import com.bookcover.archive.domain.user.User;
import com.bookcover.archive.domain.user.UserStatus;
import com.bookcover.archive.exception.ResourceNotFoundException;
import com.bookcover.archive.exception.ValidationException;
import com.bookcover.archive.repository.UserRepository;
import com.bookcover.archive.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 회원 서비스 구현체.
 *
 * Requirements: 7.7, 7.8
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ValidationException("User not found",
                        Map.of("email", "User not found with email: " + email)));
    }

    @Override
    @Transactional
    public User updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = getUserById(userId);

        if (request.displayName() != null && !request.displayName().isBlank()) {
            user.setDisplayName(request.displayName());
        }

        // websiteUrl - null이면 변경하지 않음, 빈 문자열이면 삭제
        if (request.websiteUrl() != null) {
            user.setWebsiteUrl(request.websiteUrl().isBlank() ? null : request.websiteUrl());
        }

        // portfolioUrl - null이면 변경하지 않음, 빈 문자열이면 삭제
        if (request.portfolioUrl() != null) {
            user.setPortfolioUrl(request.portfolioUrl().isBlank() ? null : request.portfolioUrl());
        }

        // snsAccounts - null이면 변경하지 않음, 빈 맵이면 삭제
        if (request.snsAccounts() != null) {
            if (request.snsAccounts().isEmpty()) {
                user.setSnsAccounts(new HashMap<>());
            } else {
                user.setSnsAccounts(new HashMap<>(request.snsAccounts()));
            }
        }

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = getUserById(userId);

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ValidationException("Invalid password",
                    Map.of("currentPassword", "Current password is incorrect"));
        }

        // 새 비밀번호가 현재 비밀번호와 같은지 확인
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ValidationException("Same password",
                    Map.of("newPassword", "New password must be different from current password"));
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Override
    public void withdraw(UUID userId, String password) {
        User user = getUserById(userId);

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ValidationException("Invalid password",
                    Map.of("password", "Password is incorrect"));
        }

        // email은 unique·not null이라 id 기반 자리표시자로 바꿔 원래 이메일을 재가입에 풀어준다.
        // 이메일이 바뀌므로 남은 액세스·리프레시 토큰도 사용자 조회에 실패해 무효가 된다.
        user.setEmail("deleted-" + user.getId() + "@deleted.invalid");
        user.setDisplayName("탈퇴한 회원");
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setWebsiteUrl(null);
        user.setPortfolioUrl(null);
        user.getSnsAccounts().clear();
        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> searchByDisplayName(String displayName, Pageable pageable) {
        return userRepository.findByDisplayNameContainingIgnoreCaseAndStatusNot(displayName, UserStatus.DELETED, pageable);
    }
}
