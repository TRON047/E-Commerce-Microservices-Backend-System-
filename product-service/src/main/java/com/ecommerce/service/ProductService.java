package com.ecommerce.service;

import com.ecommerce.dto.ProductRequest;
import com.ecommerce.dto.ProductResponse;
import com.ecommerce.model.Product;
import com.ecommerce.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    // ── CRUD ────────────────────────────────────────────────────────────────

    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new IllegalArgumentException("A product with SKU " + request.getSku() + " already exists.");
        }
        Product product = mapToEntity(request);
        Product saved = productRepository.save(product);
        log.info("Created product id={} sku={}", saved.getId(), saved.getSku());
        return mapToResponse(saved);
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ProductResponse getProduct(Long id) {
        return productRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    }

    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));

        // Only allow SKU change if it's the same product or SKU is free
        if (!product.getSku().equals(request.getSku()) && productRepository.existsBySku(request.getSku())) {
            throw new IllegalArgumentException("SKU " + request.getSku() + " is already taken.");
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setSku(request.getSku());

        return mapToResponse(productRepository.save(product));
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new EntityNotFoundException("Product not found: " + id);
        }
        productRepository.deleteById(id);
        log.info("Deleted product id={}", id);
    }

    // ── Inventory ────────────────────────────────────────────────────────────

    /**
     * Called by Order Service (via HTTP) to check and reserve stock.
     * Uses the atomic SQL UPDATE in the repository to prevent overselling.
     */
    @Transactional
    public boolean reserveStock(Long productId, int quantity) {
        int updated = productRepository.decreaseStock(productId, quantity);
        if (updated == 0) {
            log.warn("Insufficient stock for productId={} requestedQty={}", productId, quantity);
            return false;
        }
        log.info("Reserved {} units of productId={}", quantity, productId);
        return true;
    }

    // ── Mapping helpers ──────────────────────────────────────────────────────

    private Product mapToEntity(ProductRequest req) {
        return Product.builder()
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice())
                .stockQuantity(req.getStockQuantity())
                .sku(req.getSku())
                .build();
    }

    private ProductResponse mapToResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .stockQuantity(p.getStockQuantity())
                .sku(p.getSku())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
