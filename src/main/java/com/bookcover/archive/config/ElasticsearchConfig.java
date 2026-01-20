package com.bookcover.archive.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch 설정 클래스.
 * 한국어 분석기 설정 및 클라이언트 구성을 담당합니다.
 *
 * Requirements: 8.1 - 책 제목, 작가, 출판사, 디자이너 이름, ISBN으로 검색 결과를 반환
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.bookcover.archive.search")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String elasticsearchUri;

    @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
                .connectedTo(elasticsearchUri.replace("http://", "").replace("https://", ""))
                .build();
    }
}
