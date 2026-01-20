package com.bookcover.archive.domain.user;

/**
 * 회원 역할.
 * 출판사, 작가, 디자이너, 일반회원, 관리자 중 하나를 선택합니다.
 */
public enum UserRole {
    PUBLISHER,
    AUTHOR,
    DESIGNER,
    MEMBER,
    ADMIN
}
