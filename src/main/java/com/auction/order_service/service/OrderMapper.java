package com.auction.order_service.service;

import com.auction.order_service.dto.OrderRequest;
import com.auction.order_service.dto.OrderResponse;
import com.auction.order_service.dto.PurchaseResponse;
import com.auction.order_service.model.Order;
import com.auction.order_service.orderLine.OrderLine;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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

    public OrderResponse fromOrder(Order order, List<PurchaseResponse> purchasedProducts) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        return new OrderResponse(
                order.getOrderId(),
                order.getOrderReference(),
                order.getTotalAmount(),
                order.getBuyer(),
                purchasedProducts,
                order.getCreatedDate(),
                order.getLastModifiedDate()
        );
    }

    /*
    public OrderResponse fromOrder(Order order, List<PurchaseResponse> purchasedProducts) {
        return new OrderResponse(
                order.getOrderId(),
                order.getOrderReference(),
                order.getTotalAmount(),
                order.getBuyer(),
                purchasedProducts,
                order.getCreatedDate(),
                order.getLastModifiedDate()
        );
    }
    */
    /*
    public OrderResponse fromOrder(Order order) {

        List<OrderResponse.ProductDetails> orderLineDetails = order.getOrderLines().stream()
                .map(this::fromProductDetails)
                .collect(Collectors.toList());
        return new OrderResponse(order.getOrderId(),
                order.getOrderReference(),
                order.getTotalAmount(),
                order.getBuyer(),
                orderLineDetails);

        /*
        return new OrderResponse(
                order.getOrderId(),
                order.getOrderReference(),
                order.getTotalAmount(),
                order.getBuyer(),
               order.getOrderLines()

               // order.
               // order.getUsername()
        );*/
    }
/*
    private OrderResponse.ProductDetails fromOrderLine(OrderLine orderLine) {
        return new OrderResponse.ProductDetails(
                productDeatils.getProductId(),
                orderLine.getName),
                orderLine.getQuantity()
        );
    }

    */
//}
