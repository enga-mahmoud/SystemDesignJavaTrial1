package com.ecommerce.search.dto;

import lombok.Data;

@Data
public class SearchRequest {

    /** Full-text search query string (matched against name and description). */
    private String q;

    /** Filter by category ID (exact match). */
    private String category;

    /** Minimum price filter (inclusive). */
    private Double minPrice;

    /** Maximum price filter (inclusive). */
    private Double maxPrice;

    /** Zero-based page index. Defaults to 0. */
    private int page = 0;

    /** Page size. Defaults to 20. */
    private int size = 20;
}
