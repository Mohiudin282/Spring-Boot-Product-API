package com.kousenit.demo.repositories;

import com.kousenit.demo.entities.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class ProductRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    private Product laptop, mouse, keyboard;

    @BeforeEach
    void setUp(){
        laptop = new Product();
        laptop.setName("Gaming Laptop");
        laptop.setPrice(new BigDecimal("1299.99"));
        laptop.setDescription("High-performance laptop");
        laptop.setQuantity(5);
        laptop.setSku("LAP-123456");
        laptop.setContactEmail("sales@tech.com");

        mouse = new Product();
        mouse.setName("Wireless Mouse");
        mouse.setPrice(new BigDecimal("29.99"));
        mouse.setDescription("Ergonomic wireless mouse");
        mouse.setQuantity(50);
        mouse.setSku("MOU-123456");
        mouse.setContactEmail("sales@tech.com");

        keyboard = new Product();
        keyboard.setName("Mechanical Keyboard");
        keyboard.setPrice(new BigDecimal("89.99"));
        keyboard.setDescription("RGB mechanical keyboard");
        keyboard.setQuantity(20);
        keyboard.setSku("KEY-123456");
        keyboard.setContactEmail("sales@tech.com");

        entityManager.persist(laptop);
        entityManager.persist(mouse);
        entityManager.persist(keyboard);
        entityManager.flush();
    }

    @Test
    void testFindBySku(){
        Optional<Product> found = productRepository.findBySku("LAP-123456");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Gaming Laptop");
    }

    @Test
    void testFindByNameContainingIgnoreCase(){
        List<Product> found = productRepository.findByNameContainingIgnoreCase("mouse");
        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getName()).isEqualTo("Wireless Mouse");
    }

    @Test
    void testFindByPriceBetween() {
        List<Product> products = productRepository.findByPriceBetween(
                new BigDecimal("50.00"),
                new BigDecimal("100.00")
        );

        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("Mechanical Keyboard");
    }

    @Test
    void testFindLowStockProducts() {
        List<Product> lowStock = productRepository.findLowStockProducts(10);

        assertThat(lowStock).hasSize(1);
        assertThat(lowStock.get(0).getName()).isEqualTo("Gaming Laptop");
        assertThat(lowStock.get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void testFindExpensiveProducts() {
        List<Product> expensive = productRepository.findExpensiveProducts(new BigDecimal("100.00"));

        assertThat(expensive).hasSize(1);
        assertThat(expensive.getFirst().getName()).isEqualTo("Gaming Laptop");
    }

    @Test
    void testExistsBySku() {
        boolean exists = productRepository.existsBySku("MOU-123456");
        assertThat(exists).isTrue();

        boolean notExists = productRepository.existsBySku("XXX-999999");
        assertThat(notExists).isFalse();
    }

    @Test
    void testCountByQuantityLessThan() {
        long count = productRepository.countByQuantityLessThan(10);
        assertThat(count).isEqualTo(1); // Only laptop has quantity < 10
    }

}
