package com.kousenit.demo.entities;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ProductTest {
    @Test
    void testHasStockReturnsTrue() {
        Product product = new Product();
        product.setQuantity(10);

        assertThat(product.hasStock(5)).isTrue();
    }

    @Test
    void testHasStockReturnsFalse() {
        Product product = new Product();
        product.setQuantity(3);

        assertThat(product.hasStock(5)).isFalse();
    }

    @Test
    void testDecrementStockSuccess() {
        Product product = new Product();
        product.setQuantity(10);

        product.decrementStock(3);

        assertThat(product.getQuantity()).isEqualTo(7);
    }

    @Test
    void testDecrementStockThrowsExceptionWhenInsufficientStock() {
        Product product = new Product();
        product.setQuantity(2);

        assertThatThrownBy(() -> product.decrementStock(5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot decrement stock by 5");
    }

    @Test
    void testIncrementStock() {
        Product product = new Product();
        product.setQuantity(10);

        product.incrementStock(5);

        assertThat(product.getQuantity()).isEqualTo(15);
    }
}
