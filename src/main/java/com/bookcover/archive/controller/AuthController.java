package com.bookcover.archive.controller;

import com.bookcover.archive.controller.dto.auth.AuthResponse;
import com.bookcover.archive.controller.dto.auth.LoginRequest;
import com.bookcover.archive.controller.dto.auth.SignupRequest;
import com.bookcover.archive.controller.dto.auth.TokenRefreshRequest;
import com.bookcover.archive.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API 컨트롤러.
 * 회원가입, 로그인, 토큰 갱신 엔드포인트를 제공합니다.
 *
 * Requirements: 7.1, 7.8
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "인증 관련 API")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 회원가입을 수행합니다.
     *
     * Requirements: 7.1 - 회원가입 시 역할 선택
     */
    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "새 회원을 등록합니다. 역할(출판사, 작가, 디자이너, 일반회원)을 선택해야 합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)"),
            @ApiResponse(responseCode = "409", description = "이메일 중복")
    })
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        AuthResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 로그인을 수행합니다.
     */
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 리프레시 토큰으로 새 액세스 토큰을 발급합니다.
     */
    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "리프레시 토큰으로 새 액세스 토큰을 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰")
    })
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }
}
