package com.ecommerce.service;

import com.ecommerce.dto.ProductRequest;
import com.ecommerce.dto.ProductResponse;
import com.ecommerce.model.Product;
import com.ecommerce.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private ProductRequest sampleRequest;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleRequest = ProductRequest.builder()
                .name("Laptop")
                .description("A great laptop")
                .price(new BigDecimal("999.99"))
                .stockQuantity(10)
                .sku("LAP-001")
                .build();

        sampleProduct = Product.builder()
                .id(1L)
                .name("Laptop")
                .description("A great laptop")
                .price(new BigDecimal("999.99"))
                .stockQuantity(10)
                .sku("LAP-001")
                .build();
    }

    @Test
    void createProduct_success() {
        when(productRepository.existsBySku("LAP-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ProductResponse response = productService.createProduct(sampleRequest);

        assertThat(response.getName()).isEqualTo("Laptop");
        assertThat(response.getPrice()).isEqualByComparingTo("999.99");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_duplicateSku_throwsException() {
        when(productRepository.existsBySku("LAP-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(sampleRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LAP-001");

        verify(productRepository, never()).save(any());
    }

    @Test
    void getProduct_notFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void reserveStock_success() {
        when(productRepository.decreaseStock(1L, 5)).thenReturn(1);

        boolean result = productService.reserveStock(1L, 5);

        assertThat(result).isTrue();
    }

    @Test
    void reserveStock_insufficientStock_returnsFalse() {
        when(productRepository.decreaseStock(1L, 100)).thenReturn(0);

        boolean result = productService.reserveStock(1L, 100);

        assertThat(result).isFalse();
    }
}
