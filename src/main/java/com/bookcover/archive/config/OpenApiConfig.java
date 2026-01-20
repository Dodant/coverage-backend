package com.bookcover.archive.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger 문서 설정.
 * API 문서의 메타데이터, 보안 스키마, 서버 정보를 구성합니다.
 *
 * Requirements: 21.1 - OpenAPI/Swagger 문서 생성
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    @Bean
    OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(apiInfo())
                .servers(servers())
                .tags(tags())
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, securityScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("Book Cover Archive API")
                .description("""
                        북커버 디자인 아카이브 API 문서입니다.

                        ## 개요
                        북커버 디자인 아카이브는 원서와 번역서의 북커버를 수집, 비교, 공유할 수 있는 플랫폼입니다.
                        북커버 디자이너와 디자인 지망생들을 위한 영감과 레퍼런스를 제공합니다.

                        ## 인증
                        대부분의 API는 JWT Bearer 토큰 인증이 필요합니다.
                        `/auth/login` 또는 `/auth/signup` 엔드포인트를 통해 토큰을 발급받을 수 있습니다.

                        ## 주요 기능
                        - **원서/번역서 관리**: 북커버 정보 등록, 조회, 수정, 삭제
                        - **리디자인**: 기존 북커버를 재해석한 디자인 업로드
                        - **소셜 기능**: 좋아요, 댓글
                        - **컬렉션**: 북커버 모음 폴더 생성 및 공유
                        - **검색**: 다양한 조건으로 북커버 검색 및 브라우징
                        - **관리자 기능**: 신고 처리, 콘텐츠 관리, 통계 조회
                        """)
                .version("1.0.0")
                .contact(contact())
                .license(license());
    }

    private Contact contact() {
        return new Contact()
                .name("Book Cover Archive Team")
                .email("support@bookcoverarchive.com")
                .url("https://bookcoverarchive.com");
    }

    private License license() {
        return new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");
    }

    private List<Server> servers() {
        return List.of(
                new Server()
                        .url("http://localhost:8080" + contextPath)
                        .description("로컬 개발 서버"),
                new Server()
                        .url("https://api.bookcoverarchive.com")
                        .description("운영 서버")
        );
    }

    private List<Tag> tags() {
        return List.of(
                new Tag().name("Authentication").description("인증 관련 API (회원가입, 로그인, 토큰 갱신)"),
                new Tag().name("Users").description("회원 프로필 관련 API"),
                new Tag().name("Original Books").description("원서 관련 API"),
                new Tag().name("Translated Books").description("번역서 관련 API"),
                new Tag().name("Redesigns").description("리디자인 관련 API"),
                new Tag().name("Covers").description("북커버 이미지 관련 API"),
                new Tag().name("Search").description("검색 및 브라우징 관련 API"),
                new Tag().name("Social").description("좋아요, 댓글 관련 API"),
                new Tag().name("Collections").description("컬렉션 관련 API"),
                new Tag().name("Authors").description("작가 관련 API"),
                new Tag().name("Publishers").description("출판사 관련 API"),
                new Tag().name("Designers").description("디자이너 관련 API"),
                new Tag().name("Tags").description("디자인 태그 관련 API"),
                new Tag().name("Admin - Reports").description("관리자 신고 처리 API"),
                new Tag().name("Admin - Moderation").description("관리자 콘텐츠 관리 API"),
                new Tag().name("Admin - Entities").description("관리자 엔티티 관리 API"),
                new Tag().name("Admin - Dashboard").description("관리자 대시보드 API"),
                new Tag().name("Admin - Takedowns").description("관리자 저작권 삭제 요청 처리 API")
        );
    }

    private SecurityScheme securityScheme() {
        return new SecurityScheme()
                .name("bearerAuth")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT 액세스 토큰을 입력하세요. `/auth/login` 또는 `/auth/signup`에서 발급받을 수 있습니다.");
    }
}
