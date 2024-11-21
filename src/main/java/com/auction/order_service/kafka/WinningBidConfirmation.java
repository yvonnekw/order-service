package com.auction.order_service.kafka;

import com.auction.order_service.dto.ProductResponse;
import com.auction.order_service.model.PaymentMethod;
import com.auction.order_service.user.UserResponse;

import java.math.BigDecimal;
import java.util.List;

public record WinningBidConfirmation(
       String orderReference,
       BigDecimal totalAmount,
       PaymentMethod paymentMethod,
       UserResponse buyer,
       List<ProductResponse> products
) {

}
