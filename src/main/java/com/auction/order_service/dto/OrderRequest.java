package com.auction.order_service.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record OrderRequest(
        String orderReference,
        @Positive(message = "Order amount should be positive")
        BigDecimal totalAmount,
        String buyerUsername,
        @NotEmpty(message = "You should at least purchase one product")
        List<PurchaseRequest> products

){
        }
