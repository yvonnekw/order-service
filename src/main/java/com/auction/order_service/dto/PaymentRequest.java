package com.auction.order_service.dto;

import com.auction.order_service.model.PaymentMethod;

import java.math.BigDecimal;

public record PaymentRequest(
        BigDecimal amount,
        PaymentMethod paymentMethod,
        boolean isSuccessful
) {
}
