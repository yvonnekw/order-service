package com.auction.order_service.dto;

import com.auction.order_service.model.PaymentMethod;

import java.math.BigDecimal;

public record OrderResponse(
        Long orderId,
        String reference,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        Long userId
) {
}
