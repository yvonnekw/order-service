package com.auction.order_service.service;

import com.auction.order_service.dto.OrderLineResponse;
import com.auction.order_service.dto.OrderRequest;
import com.auction.order_service.dto.OrderResponse;
import com.auction.order_service.model.Order;
import org.springframework.stereotype.Service;

import java.util.List;


@Service

public class OrderMapper {

    public Order toOrder(OrderRequest request) {
        return Order.builder()
                .orderReference(request.orderReference())
                .totalAmount(request.totalAmount())
                .username(request.buyerUsername())
                .build();
    }

    public OrderResponse fromOrder(Order order, List<OrderLineResponse> orderLineResponse) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        return new OrderResponse(
                order.getOrderId(),
                order.getOrderReference(),
                order.getTotalAmount(),
                order.getUsername(),
                order.getCreatedDate(),
                order.getLastModifiedDate(),
                orderLineResponse.stream()
                        .map(orderLine -> new OrderLineResponse(
                                orderLine.orderLineId(),
                                orderLine.productId(),
                                orderLine.productName(),
                                orderLine.brandName(),
                                orderLine.description(),
                                orderLine.colour(),
                                orderLine.productSize(),
                                orderLine.startingPrice(),
                                orderLine.buyNowPrice(),
                                orderLine.quantity(),
                                orderLine.productImageUrl()
                        )).toList()
        );
    }

}

