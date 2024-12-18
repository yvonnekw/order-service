package com.auction.order_service.dto;

import java.math.BigDecimal;

public record PaymentRequest(
      //  Long orderId,
        BigDecimal amount,
        String paymentMethod
) {
}
