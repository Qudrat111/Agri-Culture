package com.agriculture.catalog.repository;

import com.agriculture.catalog.document.ProductMetadata;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * MongoDB Repository for ProductMetadata
 */
@Repository
public interface ProductMetadataRepository extends MongoRepository<ProductMetadata, String> {
    
    Optional<ProductMetadata> findByProductId(String productId);
}
