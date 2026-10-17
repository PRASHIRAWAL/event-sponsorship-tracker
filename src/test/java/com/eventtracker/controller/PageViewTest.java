package com.eventtracker.controller;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PageViewTest {

    private static final Map<String, String> SORT_KEYS = new LinkedHashMap<>();

    static {
        SORT_KEYS.put("date", "eventDate");
        SORT_KEYS.put("name", "name");
    }

    @Test
    void unknownSortKeyFallsBackToDefaultAndIdBreaksTies() {
        PageView view = PageView.of("/events", SORT_KEYS, "password", "desc", null, Map.of(), true);

        Pageable pageable = view.pageable(null);

        assertThat(view.getSortKey()).isEqualTo("date");
        assertThat(pageable.getSort().getOrderFor("eventDate").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(pageable.getSort().getOrderFor("id")).isNotNull();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getPageNumber()).isZero();
    }

    @Test
    void onlyAllowedPageSizesAreUsedAndNegativePagesBecomeFirst() {
        assertThat(PageView.of("/e", SORT_KEYS, null, null, 25, Map.of(), true).pageable(-3))
                .satisfies(pageable -> {
                    assertThat(pageable.getPageSize()).isEqualTo(25);
                    assertThat(pageable.getPageNumber()).isZero();
                });
        assertThat(PageView.of("/e", SORT_KEYS, null, null, 1000, Map.of(), true).getSize()).isEqualTo(10);
    }

    @Test
    void linksKeepFiltersDropBlankOnesAndToggleSortDirection() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("q", "tech fest");
        filters.put("status", null);
        filters.put("eventId", "");
        PageView view = PageView.of("/sponsors", SORT_KEYS, "name", "asc", null, filters, true);

        assertThat(view.url(2)).isEqualTo("/sponsors?q=tech%20fest&sort=name&dir=asc&page=2");
        assertThat(view.sortUrl("name")).isEqualTo("/sponsors?q=tech%20fest&sort=name&dir=desc");
        assertThat(view.sortUrl("date")).isEqualTo("/sponsors?q=tech%20fest&sort=date&dir=asc");
        assertThat(view.sizeUrl(50)).isEqualTo("/sponsors?q=tech%20fest&sort=name&dir=asc&size=50");
        assertThat(view.sortState("name")).isEqualTo("ascending");
        assertThat(view.sortState("date")).isEqualTo("none");
    }

    @Test
    void pageNumbersShowNeighboursAndGaps() {
        PageView view = PageView.of("/e", SORT_KEYS, null, null, null, Map.of(), true);
        Page<String> page = new PageImpl<>(List.of("x"), PageRequest.of(4, 10), 100);

        assertThat(view.withPage(page).pageNumbers()).containsExactly(0, -1, 3, 4, 5, -1, 9);
        assertThat(view.getFrom()).isEqualTo(41);
        assertThat(view.getTo()).isEqualTo(41);
    }

    @Test
    void detectsRequestsBeyondTheLastPage() {
        Page<String> beyond = new PageImpl<>(Collections.emptyList(), PageRequest.of(5, 10), 20);
        Page<String> first = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);

        assertThat(PageView.isBeyondLastPage(beyond)).isTrue();
        assertThat(PageView.isBeyondLastPage(first)).isFalse();
    }
}
