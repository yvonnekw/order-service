package com.auction.order_service.dto;

import java.math.BigDecimal;

public record PurchaseResponse(
        Long productId,
        String productName,
        String description,

        BigDecimal price,
        Integer quantity

) {


}
