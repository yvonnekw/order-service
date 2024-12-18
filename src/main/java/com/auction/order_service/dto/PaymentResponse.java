package com.auction.order_service.dto;

import java.math.BigDecimal;

public record PaymentResponse(
        Long orderId,
        BigDecimal amount,
        boolean isSuccessful
) {}
