package com.auction.order_service.dto;

import com.auction.order_service.model.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        BigDecimal amount,
        PaymentMethod paymentMethod,
        Long orderId,
        String orderReference,
        LocalDateTime createdDate,
        LocalDateTime lastModifiedDate,
        boolean isSuccessful
) {}
