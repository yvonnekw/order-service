package com.auction.order_service.dto;

public record OrderLineResponse(
        Long orderLineId,
        double quantity
) {
}
