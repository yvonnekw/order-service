package com.auction.order_service.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record OrderRequest(
        //Long orderId,
        String orderReference,
        @Positive(message = "Order amount should be positive")
        BigDecimal totalAmount,
        //@NotNull(message = "Payment method is required")
        //PaymentMethod paymentMethod,
        //@NotNull(message = "buyer username should not be a null")
        //@NotEmpty(message = "buyerId should not be empty")
        //@NotBlank(message = "buyerId should not be blank")
        String buyerUsername,
        @NotEmpty(message = "You should at least purchase one product")
        List<PurchaseRequest> products

){
        }
