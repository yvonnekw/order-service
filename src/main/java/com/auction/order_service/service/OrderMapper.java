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
                .orderReference(request.orderReference())
                .totalAmount(request.totalAmount())
                .buyer(request.buyerUsername())
                //.paymentMethod(request.paymentMethod())
                .build();
    }

    public OrderResponse fromOrder(Order order) {
        return new OrderResponse(
                order.getOrderId(),
                order.getOrderReference(),
                order.getTotalAmount(),
                order.getBuyer()
               // order.
               // order.getUsername()
        );
    }
}
