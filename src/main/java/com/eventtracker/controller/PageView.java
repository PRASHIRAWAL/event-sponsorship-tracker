package com.eventtracker.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pagination and sorting state for a list page, plus the links the template needs.
 * Sort keys are whitelisted per page (key -> entity property) so request parameters can never
 * reach the query as arbitrary property names. Links keep the current filters and drop empty ones.
 */
public final class PageView {

    public static final List<Integer> PAGE_SIZES = List.of(10, 25, 50);
    private static final int DEFAULT_SIZE = 10;

    private final String path;
    private final Map<String, Object> filters;
    private final String sortKey;
    private final String sortProperty;
    private final boolean ascending;
    private final int size;
    private Page<?> page;

    private PageView(String path, Map<String, Object> filters, String sortKey, String sortProperty,
                     boolean ascending, int size) {
        this.path = path;
        this.filters = filters;
        this.sortKey = sortKey;
        this.sortProperty = sortProperty;
        this.ascending = ascending;
        this.size = size;
    }

    /**
     * @param sortKeys         allowed sort keys mapped to entity properties; the first entry is the default
     * @param filters          current filter values (null or blank values are left out of links)
     * @param defaultAscending direction used when the request does not give one
     */
    public static PageView of(String path, Map<String, String> sortKeys, String sort, String dir, Integer size,
                              Map<String, Object> filters, boolean defaultAscending) {
        String key = sort != null && sortKeys.containsKey(sort) ? sort : sortKeys.keySet().iterator().next();
        boolean ascending = dir == null ? defaultAscending : !"desc".equalsIgnoreCase(dir);
        int pageSize = size != null && PAGE_SIZES.contains(size) ? size : DEFAULT_SIZE;
        Map<String, Object> cleaned = new LinkedHashMap<>();
        filters.forEach((name, value) -> {
            if (value != null && !value.toString().isBlank()) {
                cleaned.put(name, value);
            }
        });
        return new PageView(path, cleaned, key, sortKeys.get(key), ascending, pageSize);
    }

    /** Pageable for the requested page (negative pages become the first page), with id as a stable tie-breaker. */
    public Pageable pageable(Integer requestedPage) {
        int number = requestedPage == null || requestedPage < 0 ? 0 : requestedPage;
        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, sortProperty)
                .and(Sort.by(Sort.Direction.ASC, "id"));
        return PageRequest.of(number, size, sort);
    }

    public PageView withPage(Page<?> page) {
        this.page = page;
        return this;
    }

    /** True when the requested page is past the end (for example after deleting the last row of a page). */
    public static boolean isBeyondLastPage(Page<?> page) {
        return page.getNumber() > 0 && page.getNumber() >= page.getTotalPages();
    }

    public String url(int pageNumber) {
        return build(pageNumber, sortKey, ascending, size);
    }

    public String sizeUrl(int newSize) {
        return build(0, sortKey, ascending, newSize);
    }

    /** Link for a column header: the same column toggles the direction, a new column starts ascending. */
    public String sortUrl(String key) {
        boolean nextAscending = !key.equals(sortKey) || !ascending;
        return build(0, key, nextAscending, size);
    }

    /** "ascending", "descending" or "none", for the aria-sort attribute and the arrow icon. */
    public String sortState(String key) {
        if (!key.equals(sortKey)) {
            return "none";
        }
        return ascending ? "ascending" : "descending";
    }

    /** Query string for exports: the current filters and sort, without paging. */
    public String exportUrl(String exportPath) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(exportPath);
        filters.forEach(builder::queryParam);
        builder.queryParam("sort", sortKey).queryParam("dir", ascending ? "asc" : "desc");
        return builder.encode().toUriString();
    }

    private String build(int pageNumber, String key, boolean asc, int pageSize) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path);
        filters.forEach(builder::queryParam);
        builder.queryParam("sort", key).queryParam("dir", asc ? "asc" : "desc");
        if (pageSize != DEFAULT_SIZE) {
            builder.queryParam("size", pageSize);
        }
        if (pageNumber > 0) {
            builder.queryParam("page", pageNumber);
        }
        return builder.encode().toUriString();
    }

    /** Page numbers to show (0-based), with -1 marking a gap: e.g. 0 1 2 -1 9 when on page 1 of 10. */
    public List<Integer> pageNumbers() {
        int total = page.getTotalPages();
        int current = page.getNumber();
        List<Integer> numbers = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            if (i == 0 || i == total - 1 || Math.abs(i - current) <= 1) {
                numbers.add(i);
            } else if (!numbers.isEmpty() && numbers.get(numbers.size() - 1) != -1) {
                numbers.add(-1);
            }
        }
        return numbers;
    }

    public int getNumber() {
        return page.getNumber();
    }

    public int getTotalPages() {
        return page.getTotalPages();
    }

    public long getTotalElements() {
        return page.getTotalElements();
    }

    public boolean hasPrevious() {
        return page.hasPrevious();
    }

    public boolean hasNext() {
        return page.hasNext();
    }

    /** 1-based index of the first row shown. */
    public long getFrom() {
        return page.getTotalElements() == 0 ? 0 : (long) page.getNumber() * page.getSize() + 1;
    }

    public long getTo() {
        return (long) page.getNumber() * page.getSize() + page.getNumberOfElements();
    }

    public int getSize() {
        return size;
    }

    public List<Integer> getPageSizes() {
        return PAGE_SIZES;
    }

    public String getSortKey() {
        return sortKey;
    }

    public String getDir() {
        return ascending ? "asc" : "desc";
    }
}
