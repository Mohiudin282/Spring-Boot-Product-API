package com.kousenit.demo.services;

import com.kousenit.demo.dto.ProductRequest;
import com.kousenit.demo.dto.ProductResponse;
import com.kousenit.demo.entities.Product;
import com.kousenit.demo.exceptions.InsufficientStockException;
import com.kousenit.demo.exceptions.ProductNotFoundException;
import com.kousenit.demo.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class ProductServiceTest {
    @MockitoBean
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    private Product testProduct;
    private ProductRequest testProductRequest;

    @BeforeEach
    void setUp(){
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setDescription("Test Description");
        testProduct.setQuantity(10);
        testProduct.setSku("TST-123456");
        testProduct.setContactEmail("test@example.com");

        testProductRequest = new ProductRequest(
                "Test Product",
                new BigDecimal("99.99"),
                "Test Description",
                10,
                "TST-123456",
                "test@example.com"
        );
    }


    @Test
    @DisplayName("Should get product by id successfully")
    void testGetProductByIdSuccess() {
        //Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        //When
        ProductResponse result = productService.getProductById(1L);
        //Then
        assertThat(result)
                .isNotNull()
                .returns(1L, ProductResponse::id)
                .returns("Test Product", ProductResponse::name)
                .returns(new BigDecimal("99.99"), ProductResponse::price);
        verify(productRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when product not found")
    void testGetProductByIdNotFound() {
        //Given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());
        //When
        assertThrows(ProductNotFoundException.class,
                () -> productService.getProductById(999L));
        verify(productRepository).findById(999L);
    }

    @Test
    @DisplayName("Should create product successfully")
    void testCreateProductSuccess() {
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        ProductResponse result = productService.createProduct(testProductRequest);
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Test Product");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should reserve stock successfully")
    void testReserveStockSuccess() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        ProductResponse result = productService.reserveStock(1L, 5);

        // Then
        assertThat(result).isNotNull();
        verify(productRepository).findById(1L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw exception when insufficient stock")
    void testReserveStockInsufficientStock() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // When/Then
        assertThrows(InsufficientStockException.class,
                () -> productService.reserveStock(1L, 50)); // More than available
        verify(productRepository).findById(1L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should get all products with pagination")
    void testGetAllProducts() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Product> products = List.of(testProduct);
        Page<Product> productPage = new PageImpl<>(products, pageable, 1);
        when(productRepository.findAll(pageable)).thenReturn(productPage);

        // When
        Page<ProductResponse> result = productService.getAllProducts(pageable);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Test Product");
        verify(productRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Should delete product successfully")
    void testDeleteProductSuccess() {
        // Given
        when(productRepository.existsById(1L)).thenReturn(true);
        doNothing().when(productRepository).deleteById(1L);

        // When
        productService.deleteProduct(1L);

        // Then
        verify(productRepository).existsById(1L);
        verify(productRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent product")
    void testDeleteProductNotFound() {
        // Given
        when(productRepository.existsById(999L)).thenReturn(false);

        // When/Then
        assertThrows(ProductNotFoundException.class,
                () -> productService.deleteProduct(999L));
        verify(productRepository).existsById(999L);
        verify(productRepository, never()).deleteById(anyLong());
    }
}
