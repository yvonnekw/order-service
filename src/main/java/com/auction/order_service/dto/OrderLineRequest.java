package com.auction.order_service.dto;

public record OrderLineRequest(
        //Long orderLineId,
        Long orderId,
        Long productId,
        double quantity) {
}
