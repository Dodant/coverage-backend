package com.bookcover.archive.controller;

import com.bookcover.archive.controller.SeriesController.SeriesBookItem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SeriesOrderTest {

    private static SeriesBookItem item(String number, int year) {
        return new SeriesBookItem(null, "ORIGINAL", "t", number, year, "KR", null);
    }

    @Test
    void sortsByLeadingNumberThenTextThenYear() {
        List<String> sorted = Stream.of(item("12", 2000), item(null, 2000), item("별권", 2000),
                        item("3-2", 2000), item("3-1", 2000), item("3", 2001), item("3", 1999))
                .sorted(SeriesController.SERIES_ORDER)
                .map(b -> b.seriesNumber() + "/" + b.year())
                .toList();
        assertThat(sorted).containsExactly("3/1999", "3/2001", "3-1/2000", "3-2/2000", "12/2000", "별권/2000", "null/2000");
    }
}
