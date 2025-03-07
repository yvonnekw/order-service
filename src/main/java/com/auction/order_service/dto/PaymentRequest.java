package com.auction.order_service.dto;

import com.auction.order_service.model.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentRequest(
        //  Long orderId,
        //BigDecimal amount,
        //String paymentMethod,
        //String orderReference

        Long orderId,
        String orderReference,
        BigDecimal amount,
        PaymentMethod paymentMethod,

        // String username,
        // User User,
        //LocalDateTime createdDate,
        //LocalDateTime lastModifiedDate,
        boolean isSuccessful
) {
}
