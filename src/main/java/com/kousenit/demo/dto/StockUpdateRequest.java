package com.kousenit.demo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockUpdateRequest(
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Minimum value should be 1")
        Integer quantity
){}
