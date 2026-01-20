package com.bookcover.archive.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 레이트 리밋 설정.
 * application.yml의 rate-limit 설정을 바인딩합니다.
 */
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitConfig {

    private ActionLimit upload = new ActionLimit(10, 3600);
    private ActionLimit comment = new ActionLimit(30, 60);
    private ActionLimit like = new ActionLimit(100, 60);
    private ActionLimit report = new ActionLimit(5, 3600);

    public ActionLimit getUpload() {
        return upload;
    }

    public void setUpload(ActionLimit upload) {
        this.upload = upload;
    }

    public ActionLimit getComment() {
        return comment;
    }

    public void setComment(ActionLimit comment) {
        this.comment = comment;
    }

    public ActionLimit getLike() {
        return like;
    }

    public void setLike(ActionLimit like) {
        this.like = like;
    }

    public ActionLimit getReport() {
        return report;
    }

    public void setReport(ActionLimit report) {
        this.report = report;
    }

    /**
     * 액션별 레이트 리밋 설정.
     */
    public static class ActionLimit {
        private int limit;
        private int windowSeconds;

        public ActionLimit() {
        }

        public ActionLimit(int limit, int windowSeconds) {
            this.limit = limit;
            this.windowSeconds = windowSeconds;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public int getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(int windowSeconds) {
            this.windowSeconds = windowSeconds;
        }
    }
}
