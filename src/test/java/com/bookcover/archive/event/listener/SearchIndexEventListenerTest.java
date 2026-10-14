package com.bookcover.archive.event.listener;

import com.bookcover.archive.event.ContentEvent;
import com.bookcover.archive.event.ContentEventType;
import com.bookcover.archive.service.SearchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/** 리스너가 예외를 삼키지 않아야 KafkaConfig의 재시도·DLT가 동작한다. */
class SearchIndexEventListenerTest {

    private final SearchService searchService = mock(SearchService.class);
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final SearchIndexEventListener listener = new SearchIndexEventListener(searchService, mapper);

    @Test
    void 잘못된_JSON은_예외로_올린다() {
        assertThrows(JsonProcessingException.class, () -> listener.handleContentEvent("{not json"));
    }

    @Test
    void 색인_실패는_삼키지_않는다() throws Exception {
        doThrow(new RuntimeException("es down")).when(searchService).indexOriginalBook(any());
        String message = mapper.writeValueAsString(
                new ContentEvent(ContentEventType.ORIGINAL_BOOK_UPDATED, UUID.randomUUID(), null));

        assertThrows(RuntimeException.class, () -> listener.handleContentEvent(message));
    }
}
