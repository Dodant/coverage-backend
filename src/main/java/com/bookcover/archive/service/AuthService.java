package com.bookcover.archive.service;

import com.bookcover.archive.controller.dto.auth.AuthResponse;
import com.bookcover.archive.controller.dto.auth.LoginRequest;
import com.bookcover.archive.controller.dto.auth.SignupRequest;
import com.bookcover.archive.controller.dto.auth.TokenRefreshRequest;
import com.bookcover.archive.domain.user.User;

/**
 * 인증 서비스 인터페이스.
 * 회원가입, 로그인, 토큰 갱신, 로그아웃 기능을 제공합니다.
 *
 * Requirements: 7.1, 7.8
 */
public interface AuthService {

    /**
     * 새 회원을 등록합니다.
     *
     * @param request 회원가입 요청
     * @return 인증 응답 (토큰 포함)
     */
    AuthResponse signup(SignupRequest request);

    /**
     * 로그인을 수행합니다.
     *
     * @param request 로그인 요청
     * @return 인증 응답 (토큰 포함)
     */
    AuthResponse login(LoginRequest request);

    /**
     * 리프레시 토큰으로 새 액세스 토큰을 발급합니다.
     *
     * @param request 토큰 갱신 요청
     * @return 인증 응답 (새 토큰 포함)
     */
    AuthResponse refreshToken(TokenRefreshRequest request);

    /**
     * 리프레시 토큰을 폐기해 현재 기기에서 로그아웃합니다.
     * 액세스 토큰은 만료될 때까지 유효하다. 유효하지 않은 토큰이어도 오류 없이 끝난다.
     *
     * @param refreshToken 폐기할 리프레시 토큰
     */
    void logout(String refreshToken);

    /**
     * 이메일로 사용자를 조회합니다.
     *
     * @param email 이메일
     * @return 사용자 엔티티
     */
    User getUserByEmail(String email);
}
