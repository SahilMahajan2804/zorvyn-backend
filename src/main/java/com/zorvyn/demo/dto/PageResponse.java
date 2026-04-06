package com.zorvyn.demo.dto;

import lombok.*;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * A clean, minimal wrapper around Spring's Page object.
 * Replaces the verbose default Spring Page JSON with only the fields that matter.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {

    private List<T> content;

    private int page;          // current page (0-indexed)
    private int size;          // page size
    private long totalElements; // total records matching the filter
    private int totalPages;    // total pages
    private boolean first;     // is this the first page?
    private boolean last;      // is this the last page?

    /**
     * Static factory — wrap any Spring {@link Page} instance.
     */
    public static <T> PageResponse<T> of(Page<T> springPage) {
        return PageResponse.<T>builder()
                .content(springPage.getContent())
                .page(springPage.getNumber())
                .size(springPage.getSize())
                .totalElements(springPage.getTotalElements())
                .totalPages(springPage.getTotalPages())
                .first(springPage.isFirst())
                .last(springPage.isLast())
                .build();
    }
}
