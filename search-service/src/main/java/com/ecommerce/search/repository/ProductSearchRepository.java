package com.ecommerce.search.repository;

import com.ecommerce.search.document.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * Basic CRUD repository for ProductDocument.
 * Complex search queries are handled via ElasticsearchOperations (NativeQuery) in SearchService.
 */
@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {
    // Custom finder methods can be declared here if simple field-matching is needed.
    // Advanced full-text and filtered queries are built programmatically in SearchService.
}
