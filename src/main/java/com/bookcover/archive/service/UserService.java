package com.bookcover.archive.service;

import com.bookcover.archive.controller.dto.user.ChangePasswordRequest;
import com.bookcover.archive.controller.dto.user.UpdateProfileRequest;
import com.bookcover.archive.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * 회원 서비스 인터페이스.
 * 프로필 조회 및 수정 기능을 제공합니다.
 *
 * Requirements: 7.7, 7.8
 */
public interface UserService {

    /**
     * ID로 회원을 조회합니다.
     *
     * @param id 회원 ID
     * @return 회원 엔티티
     */
    User getUserById(UUID id);

    /**
     * 이메일로 회원을 조회합니다.
     *
     * @param email 이메일
     * @return 회원 엔티티
     */
    User getUserByEmail(String email);

    /**
     * 회원 프로필을 수정합니다.
     *
     * @param userId 회원 ID
     * @param request 수정 요청
     * @return 수정된 회원 엔티티
     */
    User updateProfile(UUID userId, UpdateProfileRequest request);

    /**
     * 비밀번호를 변경합니다.
     *
     * @param userId 회원 ID
     * @param request 비밀번호 변경 요청
     */
    void changePassword(UUID userId, ChangePasswordRequest request);

    /**
     * 회원 탈퇴. 개인정보를 지우고 "탈퇴한 회원"으로 익명화한다.
     * 등록한 책·수정 이력·댓글은 위키 기여로 남긴다. 같은 이메일로 다시 가입할 수 있다.
     *
     * @param userId 회원 ID
     * @param password 본인 확인용 현재 비밀번호
     */
    void withdraw(UUID userId, String password);

    /**
     * 회원 목록을 조회합니다.
     *
     * @param pageable 페이징 정보
     * @return 회원 페이지
     */
    Page<User> getUsers(Pageable pageable);

    /**
     * 표시 이름으로 회원을 검색합니다.
     *
     * @param displayName 검색어
     * @param pageable 페이징 정보
     * @return 회원 페이지
     */
    Page<User> searchByDisplayName(String displayName, Pageable pageable);
}
