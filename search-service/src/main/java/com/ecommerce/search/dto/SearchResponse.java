package com.ecommerce.search.dto;

import com.ecommerce.search.document.ProductDocument;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchResponse {

    /** Matching product documents for the current page. */
    private List<ProductDocument> products;

    /** Total number of matching documents across all pages. */
    private long totalElements;

    /** Total number of pages. */
    private int totalPages;

    /** Current zero-based page index. */
    private int currentPage;
}
