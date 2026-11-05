package com.bookcover.archive.service.impl;

import com.bookcover.archive.service.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

/** 원서·번역서 등록/수정과 표지 변형·관리자 편집의 길이 제한 및 구형 요청 호환성을 확인한다. */
class CopyrightMetadataValidationTest {
    private static final List<Class<?>> TYPES = List.of(UpdateCoverMetadataRequest.class, AddCoverRequest.class,
            CreateOriginalBookRequest.BookCoverInfo.class, CreateTranslatedBookRequest.BookCoverInfo.class,
            UpdateOriginalBookRequest.BookCoverUpdateInfo.class, UpdateTranslatedBookRequest.BookCoverUpdateInfo.class);
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test void everyCoverInputAccepts500CharactersAndRejects501() throws Exception {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (Class<?> type : TYPES) {
                String base = mapper.writeValueAsString(Map.of("imageUrl", "https://example.org/a.png", "thumbnailUrl", "https://example.org/t.png",
                        "originalFileName", "cover.png", "variantType", "UNKNOWN", "ribbonStatus", "UNKNOWN", "copyrightHolder", "권".repeat(500)));
                Object valid = mapper.readerFor(type).without(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(base);
                assertThat(validator.validate(valid)).as(type.getName()).isEmpty();
                Object invalid = mapper.readerFor(type).without(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .readValue(base.replace("권".repeat(500), "권".repeat(501)));
                assertThat(validator.validate(invalid)).as(type.getName()).anyMatch(v -> v.getPropertyPath().toString().equals("copyrightHolder"));
            }
        }
    }

    @Test void oldCoverRequestsCanOmitCopyrightHolder() throws Exception {
        for (Class<?> type : TYPES) {
            Object value = mapper.readValue("{}", type);
            assertThat(type.getMethod("copyrightHolder").invoke(value)).as(type.getName()).isNull();
        }
    }
}
