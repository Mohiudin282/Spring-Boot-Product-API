package com.kousenit.demo.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Integer quantity;

    @Column(unique = true, nullable = false)
    private String sku;

    private String contactEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate(){
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate(){
        updatedAt = LocalDateTime.now();
    }

    public boolean hasStock(int requiredQuantity){
        return this.quantity >= requiredQuantity;
    }

    public void decrementStock(int amount){
        if(!hasStock(amount)){
            throw new IllegalArgumentException(
                    String.format("Cannot decrement stock by %d. Only %d available",amount, this.quantity));
        }
        this.quantity -= amount;
    }

    public void incrementStock(int amount){
        this.quantity += amount;
    }


}
