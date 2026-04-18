package com.ecommerce.repository;

import com.ecommerce.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    /**
     * Reduces stock atomically in the database layer.
     * Returns the number of rows updated (1 = success, 0 = out of stock).
     */
    @Modifying
    @Query("""
        UPDATE Product p
        SET p.stockQuantity = p.stockQuantity - :quantity
        WHERE p.id = :id
          AND p.stockQuantity >= :quantity
    """)
    int decreaseStock(@Param("id") Long id, @Param("quantity") int quantity);
}
