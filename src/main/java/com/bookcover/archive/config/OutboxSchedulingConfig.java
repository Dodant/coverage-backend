package com.bookcover.archive.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 콘텐츠 이벤트 재전송 작업을 활성화한다. */
@Configuration
@EnableScheduling
public class OutboxSchedulingConfig {}
