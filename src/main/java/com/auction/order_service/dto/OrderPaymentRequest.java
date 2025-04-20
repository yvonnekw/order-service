package com.auction.order_service.dto;

import jakarta.validation.Valid;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class OrderPaymentRequest {
    @Valid
    private PaymentRequest paymentRequest;

    @Valid
   private OrderRequest orderRequest;
}