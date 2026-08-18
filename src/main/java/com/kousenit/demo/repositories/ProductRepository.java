package com.kousenit.demo.repositories;

import com.kousenit.demo.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Derived query methods (Spring Data generates implementation)
    Optional<Product> findBySku(String sku);
    List<Product> findByNameContainingIgnoreCase(String name);
    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    List<Product> findByNameContainingIgnoreCaseAndPriceBetween(String name, BigDecimal minPrice, BigDecimal maxPrice);

    boolean existsBySku(String sku);
    long countByQuantityLessThan(Integer quantity);

    // Custom JPQL queries
    @Query("SELECT p FROM Product p WHERE p.quantity < :threshold ORDER BY p.quantity ASC")
    List<Product> findLowStockProducts(@Param("threshold") Integer threshold);

    @Query("SELECT p FROM Product p WHERE p.price >= :minPrice ORDER BY p.price DESC")
    List<Product> findExpensiveProducts(@Param("minPrice") BigDecimal price);

    // Modifying queries for batch operations
    @Modifying
    @Query("UPDATE Product p SET p.quantity = p.quantity - :amount WHERE p.id = :id")
    List<Product> decrementStock(@Param("amount") Integer amount, @Param("id") Long id);

    @Modifying
    @Query("UPDATE Product p SET p.quantity = p.quantity + :amount WHERE p.id = :id")
    void incrementStock(@Param("id") Long id, @Param("amount") Integer amount);

    // Native SQL query
    @Query(value = "SELECT * FROM products WHERE price > :price AND quantity > 0 ORDER BY created_at DESC LIMIT :limit",
            nativeQuery = true)
    List<Product> findRecentProductsInStock(@Param("price") BigDecimal price, @Param("limit") int limit);
}
