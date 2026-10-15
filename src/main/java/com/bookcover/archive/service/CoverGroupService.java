package com.bookcover.archive.service;

import com.bookcover.archive.domain.book.BookCover;
import java.util.UUID;

/**
 * 비슷한 표지 묶음 관리 (관리자 전용).
 * 언어가 달라도 디자인이 비슷한 표지를 사람이 묶는다. 묶음은 표지들이 같은 similarGroupId를 갖는 것이다.
 */
public interface CoverGroupService {

    /**
     * coverId 표지를 withCoverId 표지의 묶음으로 옮긴다. withCoverId가 null이면 묶음에서 뺀다.
     * 표지가 1개만 남은 묶음은 풀린다.
     */
    BookCover moveToGroup(UUID coverId, UUID withCoverId);
}
