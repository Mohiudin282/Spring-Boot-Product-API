package com.kousenit.demo;


import com.kousenit.demo.controllers.GlobalExceptionHandlers;
import com.kousenit.demo.dto.ProductRequest;
import com.kousenit.demo.dto.ProductResponse;
import com.kousenit.demo.dto.StockUpdateRequest;
import com.kousenit.demo.repositories.ProductRepository;
import com.kousenit.demo.services.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ShoppingApplicationIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp(){
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("Should perform complete lifecycle operations")
    void testCompleteLifecycle() throws Exception{
        ProductRequest createRequest = new ProductRequest(
                "Integration Test Product",
                new BigDecimal("199.00"),
                "A product for integration testing",
                25,
                "INT-123456",
                "integration@example.com");

        String createResponse = mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Integration Test Product"))
                .andExpect(jsonPath("$.price").value(199.00))
                .andExpect(jsonPath("$.description").value("A product for integration testing"))
                .andExpect(jsonPath("$.quantity").value(25))
                .andExpect(jsonPath("$.sku").value("INT-123456"))
                .andExpect(jsonPath("$.contactEmail").value("integration@example.com"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        ProductResponse createdProduct = objectMapper.readValue(createResponse, ProductResponse.class);
        Long productId = createdProduct.id();

        assertThat(productRepository.count()).isEqualTo(1);

        // Step 2: Get the created product
        mockMvc.perform(get("/api/v1/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value("Integration Test Product"));

        // Step 3: Update the product
        ProductRequest updatedProduct = new ProductRequest(
                "Updated Integration Product",
                new BigDecimal("249.99"),
                "Updated description",
                30,
                "INT-123456",
                "updated@example.com"
        );
        mockMvc.perform(put("/api/v1/products/" + productId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedProduct)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Integration Product"))
                .andExpect(jsonPath("$.price").value(249.99))
                .andExpect(jsonPath("$.quantity").value(30));

        // Step 4: Reserve Stock
        StockUpdateRequest updateStock = new StockUpdateRequest(10);
        mockMvc.perform(post("/api/v1/products/" + productId + "/reserve-stock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateStock)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(20));

        // Step 5: Delete the product
        mockMvc.perform(delete("/api/v1/products/" + productId))
                .andExpect(status().isNoContent());

        // Verify deletion
        assertThat(productRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle duplicate SKU with HTTP 409 Conflict")
    void testDuplicateSkuHandling() throws Exception {
        // Create first product
        ProductRequest firstProduct = new ProductRequest(
                "First Product",
                new BigDecimal("99.99"),
                "First product description",
                10,
                "DUP-123456",
                "first@example.com"
        );

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstProduct)))
                .andExpect(status().isCreated());

        // Try to create second product with same SKU
        ProductRequest duplicateProduct = new ProductRequest(
                "Second Product",
                new BigDecimal("149.99"),
                "Second product description",
                5,
                "DUP-123456", // Same SKU
                "second@example.com"
        );

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateProduct)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Duplicate SKU"));
    }

    @Test
    @DisplayName("Should handle validation errors with detailed response")
    void testValidationErrors() throws Exception {
        ProductRequest invalidProduct = new ProductRequest(
                "AB", // Too short
                new BigDecimal("0.00"), // Too low
                null,
                -5, // Negative
                "INVALID", // Wrong format
                "not-an-email" // Invalid email
        );

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidProduct)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").isArray())
                .andExpect(jsonPath("$.validationErrors.length()").value(greaterThan(0)));
    }

    @Test
    @DisplayName("Should handle insufficient stock with detailed error")
    void testInsufficientStockError() throws Exception {
        // Create product with limited stock
        ProductRequest product = new ProductRequest(
                "Low Stock Product",
                new BigDecimal("99.99"),
                "Product with low stock",
                5,
                "LOW-123456",
                "stock@example.com"
        );

        String response = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ProductResponse created = objectMapper.readValue(response, ProductResponse.class);

        // Try to reserve more than available
        StockUpdateRequest excessiveReservation = new StockUpdateRequest(10);

        mockMvc.perform(post("/api/v1/products/" + created.id() + "/reserve-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excessiveReservation)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Insufficient Stock"))
                .andExpect(jsonPath("$.productId").value(created.id()))
                .andExpect(jsonPath("$.requestedQuantity").value(10))
                .andExpect(jsonPath("$.availableQuantity").value(5));
    }
}
