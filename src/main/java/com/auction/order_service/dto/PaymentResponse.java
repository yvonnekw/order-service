package com.auction.order_service.dto;

import com.auction.order_service.model.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        //Long orderId,
        //BigDecimal amount,
        //boolean isSuccessful
        BigDecimal amount,
        PaymentMethod paymentMethod,
        Long orderId,
        String orderReference,
        // String username,
        // User User,
        LocalDateTime createdDate,
        LocalDateTime lastModifiedDate,
        boolean isSuccessful
) {}
