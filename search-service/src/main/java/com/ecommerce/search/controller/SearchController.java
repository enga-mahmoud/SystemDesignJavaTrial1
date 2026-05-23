package com.ecommerce.search.controller;

import com.ecommerce.search.document.ProductDocument;
import com.ecommerce.search.dto.SearchRequest;
import com.ecommerce.search.dto.SearchResponse;
import com.ecommerce.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * Full-text product search with optional filters.
     *
     * <p>Example: {@code GET /api/search?q=laptop&category=electronics&minPrice=500&maxPrice=2000&page=0&size=20}
     *
     * @param q         optional free-text query (matched against name and description)
     * @param category  optional category ID filter
     * @param minPrice  optional minimum price (inclusive)
     * @param maxPrice  optional maximum price (inclusive)
     * @param page      zero-based page index (default 0)
     * @param size      page size (default 20)
     * @return paginated search results
     */
    @GetMapping
    public ResponseEntity<SearchResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        SearchRequest request = new SearchRequest();
        request.setQ(q);
        request.setCategory(category);
        request.setMinPrice(minPrice);
        request.setMaxPrice(maxPrice);
        request.setPage(page);
        request.setSize(size);

        return ResponseEntity.ok(searchService.search(request));
    }

    /**
     * Autocomplete endpoint — returns up to 10 product name suggestions for the given prefix.
     *
     * <p>Example: {@code GET /api/search/autocomplete?q=lapt}
     *
     * @param q prefix string to match against product names
     * @return list of matching product name strings
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> autocomplete(@RequestParam String q) {
        return ResponseEntity.ok(searchService.autocomplete(q));
    }
}
