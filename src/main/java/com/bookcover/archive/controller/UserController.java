package com.bookcover.archive.controller;

import com.bookcover.archive.config.security.CustomUserDetails;
import com.bookcover.archive.config.security.SecurityUtils;
import com.bookcover.archive.controller.dto.user.ChangePasswordRequest;
import com.bookcover.archive.controller.dto.user.UpdateProfileRequest;
import com.bookcover.archive.controller.dto.user.UserProfileResponse;
import com.bookcover.archive.domain.user.User;
import com.bookcover.archive.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 회원 API 컨트롤러.
 * 프로필 조회 및 수정 엔드포인트를 제공합니다.
 *
 * Requirements: 7.7, 7.8
 */
@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "회원 관련 API")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 현재 로그인한 사용자의 프로필을 조회합니다.
     *
     * Requirements: 7.7 - 역할, 칭호, 기여 횟수, 웹사이트, 포트폴리오, SNS 정보 표시
     */
    @GetMapping("/me")
    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 사용자의 프로필을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<UserProfileResponse> getMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userService.getUserById(userDetails.getId());
        return ResponseEntity.ok(UserProfileResponse.from(user));
    }

    /**
     * 현재 로그인한 사용자의 프로필을 수정합니다.
     *
     * Requirements: 7.8 - 회원이 프로필 정보를 수정할 수 있게 함
     */
    @PutMapping("/me")
    @Operation(summary = "내 프로필 수정", description = "현재 로그인한 사용자의 프로필을 수정합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        User user = userService.updateProfile(userDetails.getId(), request);
        return ResponseEntity.ok(UserProfileResponse.from(user));
    }

    /**
     * 비밀번호를 변경합니다.
     */
    @PutMapping("/me/password")
    @Operation(summary = "비밀번호 변경", description = "현재 로그인한 사용자의 비밀번호를 변경합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (현재 비밀번호 불일치 등)"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userDetails.getId(), request);
        return ResponseEntity.ok().build();
    }

    /**
     * 회원 탈퇴. 본인 확인을 위해 현재 비밀번호를 받는다.
     */
    @DeleteMapping("/me")
    @Operation(summary = "회원 탈퇴", description = "개인정보를 지우고 '탈퇴한 회원'으로 익명화합니다. 등록한 책·수정 이력·댓글은 남습니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "탈퇴 완료"),
            @ApiResponse(responseCode = "400", description = "비밀번호 불일치"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody WithdrawRequest request) {
        userService.withdraw(userDetails.getId(), request.password());
        return ResponseEntity.noContent().build();
    }

    public record WithdrawRequest(@NotBlank(message = "비밀번호를 입력해 주세요") String password) {
    }

    /**
     * 특정 사용자의 공개 프로필을 조회합니다.
     */
    @GetMapping("/{userId}")
    @Operation(summary = "사용자 프로필 조회", description = "특정 사용자의 공개 프로필을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable UUID userId) {
        User user = userService.getUserById(userId);
        // 다른 사용자의 프로필은 공개 정보만 반환
        if (SecurityUtils.getCurrentUserId().map(id -> id.equals(userId)).orElse(false)) {
            return ResponseEntity.ok(UserProfileResponse.from(user));
        }
        return ResponseEntity.ok(UserProfileResponse.publicFrom(user));
    }

    /**
     * 사용자를 검색합니다.
     */
    @GetMapping("/search")
    @Operation(summary = "사용자 검색", description = "표시 이름으로 사용자를 검색합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공")
    })
    public ResponseEntity<Page<UserProfileResponse>> searchUsers(
            @RequestParam String displayName,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<User> users = userService.searchByDisplayName(displayName, pageable);
        Page<UserProfileResponse> response = users.map(UserProfileResponse::publicFrom);
        return ResponseEntity.ok(response);
    }

    /**
     * 관리자 전용: 모든 사용자 목록을 조회합니다.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "사용자 목록 조회 (관리자)", description = "모든 사용자 목록을 조회합니다. 관리자만 접근 가능합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<Page<UserProfileResponse>> getAllUsers(@PageableDefault(size = 20) Pageable pageable) {
        Page<User> users = userService.getUsers(pageable);
        Page<UserProfileResponse> response = users.map(UserProfileResponse::from);
        return ResponseEntity.ok(response);
    }
}
