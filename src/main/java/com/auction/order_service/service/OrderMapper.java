package com.auction.order_service.service;

import com.auction.order_service.dto.OrderRequest;
import com.auction.order_service.dto.OrderResponse;
import com.auction.order_service.model.Order;
import org.springframework.stereotype.Service;

@Service

public class OrderMapper {

    public Order toOrder(OrderRequest request) {
        return Order.builder()
                //.orderId(request.orderId())
                .reference(request.reference())
                .totalAmount(request.totalAmount())
                .paymentMethod(request.paymentMethod())
                .build();
    }

    public OrderResponse fromOrder(Order order) {
        return new OrderResponse(
                order.getOrderId(),
                order.getReference(),
                order.getTotalAmount(),
                order.getPaymentMethod()
               // order.getUsername()
        );
    }
}
