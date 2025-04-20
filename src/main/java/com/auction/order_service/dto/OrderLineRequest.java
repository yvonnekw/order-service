package com.auction.order_service.dto;

import java.math.BigDecimal;

public record OrderLineRequest(
        Long orderId,
        Long productId,
        Integer quantity,
        String productName,
        String brandName,
        String description,
        String colour,
        String productSize,
        BigDecimal startingPrice,
        BigDecimal buyNowPrice,
        String productImageUrl) {
}
