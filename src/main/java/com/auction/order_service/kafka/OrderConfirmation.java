package com.auction.order_service.kafka;

import com.auction.order_service.dto.PurchaseResponse;
import com.auction.order_service.model.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;

public record OrderConfirmation(
        String orderReference,
        BigDecimal totalAmount,
        //PaymentMethod paymentMethod,
        String username,
        String firstName,
        String lastName,
        String email,
        List<PurchaseResponse> products
) {

}
