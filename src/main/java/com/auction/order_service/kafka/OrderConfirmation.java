package com.auction.order_service.kafka;

import com.auction.order_service.dto.PurchaseResponse;
import com.auction.order_service.model.PaymentMethod;
import com.auction.order_service.user.UserResponse;

import java.math.BigDecimal;
import java.util.List;

public record OrderConfirmation(
       String orderReference,
       BigDecimal totalAmount,
       PaymentMethod paymentMethod,
       UserResponse buyer,
       List<PurchaseResponse> products
) {

}
