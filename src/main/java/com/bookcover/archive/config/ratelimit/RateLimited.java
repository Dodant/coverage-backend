package com.bookcover.archive.config.ratelimit;

import com.bookcover.archive.service.RateLimitAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 레이트 리밋을 적용할 메서드에 사용하는 어노테이션.
 *
 * Requirements:
 * - 18.2: 동일 사용자가 짧은 시간 내 반복 신고를 하면 신고를 제한하고 캡차를 요구한다
 * - 18.3: 동일 사용자가 짧은 시간 내 대량 업로드를 시도하면 업로드를 제한한다
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {

    /**
     * 레이트 리밋을 적용할 액션 유형.
     */
    RateLimitAction action();
}
