package com.bookcover.archive.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 설정 클래스.
 * JPA Auditing을 활성화하여 엔티티의 생성일시, 수정일시를 자동으로 관리합니다.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
