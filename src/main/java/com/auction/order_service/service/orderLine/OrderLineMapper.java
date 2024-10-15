package com.auction.order_service.service.orderLine;

import com.auction.order_service.dto.OrderLineRequest;
import com.auction.order_service.dto.OrderLineResponse;
import com.auction.order_service.model.Order;
import com.auction.order_service.orderLine.OrderLine;
import org.springframework.stereotype.Service;

@Service
public class OrderLineMapper {
    public OrderLine toOrderLine(OrderLineRequest orderLineRequest) {
        return OrderLine.builder()
                .orderLineId(orderLineRequest.orderLineId())
                .quantity(orderLineRequest.quantity())
                .order(
                        Order.builder()
                                .orderId(orderLineRequest.orderId())
                                .build()
                )
                .productId(orderLineRequest.productId())
                .build();
    }

    public OrderLineResponse toOrderLineResponse(OrderLine orderLine) {
        return new OrderLineResponse(orderLine.getOrderLineId(), orderLine.getQuantity());
    }
}
