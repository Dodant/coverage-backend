package com.bookcover.archive.service.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** 검토 시점의 관계 ID를 함께 보내 적용 직전 변경 충돌을 확인한다. */
public record RelationshipRequest(@NotNull Action action, UUID targetId,
        UUID expectedOriginalId, UUID expectedWorkId, UUID expectedTargetWorkId) {
    public enum Action { MOVE_TRANSLATION, JOIN_WORK, LEAVE_WORK }
}
