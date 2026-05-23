package com.ecommerce.search.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.json.JsonData;
import com.ecommerce.search.document.ProductDocument;
import com.ecommerce.search.dto.SearchRequest;
import com.ecommerce.search.dto.SearchResponse;
import com.ecommerce.search.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final ProductSearchRepository searchRepository;

    /**
     * Executes a paginated, filtered full-text product search.
     *
     * <p>Query strategy:
     * <ul>
     *   <li>If {@code q} is present: multi-match on {@code name} (boosted x2) and {@code description}.</li>
     *   <li>If {@code q} is absent: match-all so all active products are returned.</li>
     *   <li>Optional filters: category (term), price range (range), status = ACTIVE (always applied).</li>
     * </ul>
     */
    public SearchResponse search(SearchRequest request) {
        List<Query> mustQueries = new ArrayList<>();
        List<Query> filterQueries = new ArrayList<>();

        // --- must clause: full-text search ---
        if (request.getQ() != null && !request.getQ().isBlank()) {
            Query multiMatchQuery = NativeQuery.builder()
                    .withQuery(q -> q.multiMatch(mm -> mm
                            .fields("name^2", "description")
                            .query(request.getQ())
                            .type(TextQueryType.BestFields)))
                    .build()
                    .getQuery();
            mustQueries.add(multiMatchQuery);
        }

        // --- filter clause: category ---
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            filterQueries.add(NativeQuery.builder()
                    .withQuery(q -> q.term(t -> t
                            .field("categoryId")
                            .value(request.getCategory())))
                    .build()
                    .getQuery());
        }

        // --- filter clause: price range ---
        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            filterQueries.add(NativeQuery.builder()
                    .withQuery(q -> q.range(r -> {
                        r.field("price");
                        if (request.getMinPrice() != null) {
                            r.gte(JsonData.of(request.getMinPrice()));
                        }
                        if (request.getMaxPrice() != null) {
                            r.lte(JsonData.of(request.getMaxPrice()));
                        }
                        return r;
                    }))
                    .build()
                    .getQuery());
        }

        // --- filter clause: only ACTIVE products (always applied) ---
        filterQueries.add(NativeQuery.builder()
                .withQuery(q -> q.term(t -> t
                        .field("status")
                        .value("ACTIVE")))
                .build()
                .getQuery());

        // --- assemble the bool query ---
        final List<Query> finalMustQueries = mustQueries;
        final List<Query> finalFilterQueries = filterQueries;

        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> {
                    if (!finalMustQueries.isEmpty()) {
                        b.must(finalMustQueries);
                    } else {
                        b.must(m -> m.matchAll(ma -> ma));
                    }
                    b.filter(finalFilterQueries);
                    return b;
                }))
                .withPageable(PageRequest.of(request.getPage(), request.getSize()))
                .build();

        SearchHits<ProductDocument> hits = elasticsearchOperations.search(searchQuery, ProductDocument.class);

        List<ProductDocument> products = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        int totalPages = (request.getSize() > 0)
                ? (int) Math.ceil((double) hits.getTotalHits() / request.getSize())
                : 0;

        return SearchResponse.builder()
                .products(products)
                .totalElements(hits.getTotalHits())
                .totalPages(totalPages)
                .currentPage(request.getPage())
                .build();
    }

    /**
     * Returns up to 10 product name suggestions that begin with the given prefix.
     * Uses a match_phrase_prefix query so partial words are handled gracefully.
     */
    public List<String> autocomplete(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return List.of();
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.matchPhrasePrefix(mpp -> mpp
                        .field("name")
                        .query(prefix)))
                .withPageable(PageRequest.of(0, 10))
                .build();

        SearchHits<ProductDocument> hits = elasticsearchOperations.search(query, ProductDocument.class);

        return hits.getSearchHits().stream()
                .map(hit -> hit.getContent().getName())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Indexes (creates or updates) a product document in Elasticsearch.
     * The method is annotated with {@code @Transactional} to mark a logical unit of work,
     * even though Elasticsearch operations are not transactional in the RDBMS sense.
     */
    @Transactional
    public void indexProduct(ProductDocument doc) {
        searchRepository.save(doc);
        log.info("Indexed product {} in Elasticsearch", doc.getId());
    }

    /**
     * Removes a product document from the Elasticsearch index by its ID.
     */
    public void deleteProduct(String productId) {
        searchRepository.deleteById(productId);
        log.info("Deleted product {} from Elasticsearch", productId);
    }
}
