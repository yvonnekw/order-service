package com.auction.order_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PurchaseRequest(
        @NotNull(message = "Product is mandatory")
        Long productId,
        @Positive(message = "Quantity is mandatory")
        int quantity,
        String productName,
        String brandName,
        String description,
        String colour,
        String productSize,
        BigDecimal startingPrice,
        BigDecimal buyNowPrice,
        String productImageUrl

) {


}
