package com.auction.order_service.dto;

public record ProductResponse(
        Long productId,
        double quantity,
        boolean isSold,
        boolean isAvailableForBuyNow

) {

}
