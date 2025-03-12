package com.auction.order_service.dto;


import com.auction.order_service.orderLine.OrderLine;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long orderId,
        String reference,
        BigDecimal totalAmount,
        String Buyer,
        //List<OrderLine> orderLines
       // PaymentMethod paymentMethod
       // Long userId
        List<PurchaseResponse> purchasedProducts,
        LocalDateTime createdDate,
        LocalDateTime lastModifiedDate
) {

}
