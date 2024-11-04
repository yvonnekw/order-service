package com.auction.order_service.dto;

import com.auction.order_service.model.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record OrderRequest(
        //Long orderId,
        String reference,
        @Positive(message = "Order amount should be positive")
        BigDecimal totalAmount,
        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,
        //@NotNull(message = "buyerId should not be a null")
        //@NotEmpty(message = "buyerId should not be empty")
        //@NotBlank(message = "buyerId should not be blank")
        Long userId,
        @NotEmpty(message = "You should at least purchase one product")
        List<PurchaseRequest> products

){
        }
