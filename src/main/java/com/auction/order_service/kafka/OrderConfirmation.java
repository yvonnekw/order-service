package com.auction.order_service.kafka;

import com.auction.order_service.dto.PurchaseResponse;

import java.math.BigDecimal;
import java.util.List;

public record OrderConfirmation(
        String orderReference,
        BigDecimal totalAmount,
        String username,
        String firstName,
        String lastName,
        String email,
        List<PurchaseResponse> products
) {

}
