package com.auction.order_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long orderId,
        String reference,
        BigDecimal totalAmount,
        String Buyer,
        LocalDateTime createdDate,
        LocalDateTime lastModifiedDate,
        List<OrderLineResponse> orderLineResponse

) {

}
