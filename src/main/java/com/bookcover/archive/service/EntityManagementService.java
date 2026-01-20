package com.bookcover.archive.service;

import com.bookcover.archive.domain.contributor.Author;
import com.bookcover.archive.domain.contributor.Designer;
import com.bookcover.archive.domain.contributor.Publisher;

import java.util.Set;
import java.util.UUID;

/**
 * 엔티티 관리 서비스 인터페이스.
 * 엔티티 병합 및 별칭 관리 기능을 제공합니다.
 *
 * Requirements:
 * - 19.5: 엔티티 병합 기능을 제공한다
 * - 19.6: 출판사/작가/디자이너에 대한 별칭(alias) 관리 기능을 제공한다
 */
public interface EntityManagementService {

    // 작가 별칭 관리

    /**
     * 작가에 별칭을 추가합니다.
     */
    Author addAuthorAlias(UUID authorId, String alias);

    /**
     * 작가에서 별칭을 제거합니다.
     */
    Author removeAuthorAlias(UUID authorId, String alias);

    /**
     * 작가의 별칭 목록을 조회합니다.
     */
    Set<String> getAuthorAliases(UUID authorId);

    // 출판사 별칭 관리

    /**
     * 출판사에 별칭을 추가합니다.
     */
    Publisher addPublisherAlias(UUID publisherId, String alias);

    /**
     * 출판사에서 별칭을 제거합니다.
     */
    Publisher removePublisherAlias(UUID publisherId, String alias);

    /**
     * 출판사의 별칭 목록을 조회합니다.
     */
    Set<String> getPublisherAliases(UUID publisherId);

    // 디자이너 별칭 관리

    /**
     * 디자이너에 별칭을 추가합니다.
     */
    Designer addDesignerAlias(UUID designerId, String alias);

    /**
     * 디자이너에서 별칭을 제거합니다.
     */
    Designer removeDesignerAlias(UUID designerId, String alias);

    /**
     * 디자이너의 별칭 목록을 조회합니다.
     */
    Set<String> getDesignerAliases(UUID designerId);

    // 엔티티 병합

    /**
     * 두 작가를 병합합니다.
     * source 작가의 모든 책을 target 작가로 이전하고 source를 삭제합니다.
     *
     * @param sourceId 병합될 작가 ID (삭제됨)
     * @param targetId 병합 대상 작가 ID (유지됨)
     * @param performedById 수행자 ID
     */
    void mergeAuthors(UUID sourceId, UUID targetId, UUID performedById);

    /**
     * 두 출판사를 병합합니다.
     * source 출판사의 모든 책을 target 출판사로 이전하고 source를 삭제합니다.
     *
     * @param sourceId 병합될 출판사 ID (삭제됨)
     * @param targetId 병합 대상 출판사 ID (유지됨)
     * @param performedById 수행자 ID
     */
    void mergePublishers(UUID sourceId, UUID targetId, UUID performedById);

    /**
     * 두 디자이너를 병합합니다.
     * source 디자이너의 모든 커버를 target 디자이너로 이전하고 source를 삭제합니다.
     *
     * @param sourceId 병합될 디자이너 ID (삭제됨)
     * @param targetId 병합 대상 디자이너 ID (유지됨)
     * @param performedById 수행자 ID
     */
    void mergeDesigners(UUID sourceId, UUID targetId, UUID performedById);
}
