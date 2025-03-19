package com.auction.order_service.service;

import com.auction.order_service.dto.OrderLineResponse;
import com.auction.order_service.dto.OrderRequest;
import com.auction.order_service.dto.OrderResponse;
import com.auction.order_service.dto.PurchaseResponse;
import com.auction.order_service.model.Order;
import com.auction.order_service.orderLine.OrderLine;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service

public class OrderMapper {

    public Order toOrder(OrderRequest request) {
        return Order.builder()
                //.orderId(request.orderId())
                .orderReference(request.orderReference())
                .totalAmount(request.totalAmount())
                .username(request.buyerUsername())
                //.paymentMethod(request.paymentMethod())
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
