package com.agriculture.catalog.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Product metadata document stored in MongoDB for flexible schema.
 * Contains extended attributes, reviews, images, etc.
 */
@Document(collection = "product_metadata")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductMetadata {
    
    @Id
    private String id;
    
    private String productId; // Reference to SQL Product entity
    
    private List<String> images;
    
    private List<String> tags;
    
    private Map<String, Object> attributes; // Flexible schema for custom attributes
    
    private List<Review> reviews;
    
    private Double averageRating;
    
    private Integer reviewCount;
    
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    private Instant updatedAt;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Review {
        private String userId;
        private Integer rating;
        private String comment;
        private Instant createdAt;
    }
}
