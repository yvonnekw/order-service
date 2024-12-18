package com.auction.order_service.dto;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Setter
@Getter
public class OrderPaymentRequest {
    @Valid
    private PaymentRequest paymentRequest;

    @Valid
   private OrderRequest orderRequest;
    //private Long orderId;
}