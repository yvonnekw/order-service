package com.auction.order_service.dto;

import java.math.BigDecimal;

public record OrderLineResponse(
        Long orderLineId,
        Long productId,
        String productName,
        String brandName,
        String description,
        String colour,
        String productSize,
        BigDecimal startingPrice,
        BigDecimal buyNowPrice,
        double quantity,
        String productImageUrl
) {
}
