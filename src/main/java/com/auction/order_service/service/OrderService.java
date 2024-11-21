package com.auction.order_service.service;

import com.auction.order_service.dto.OrderRequest;
import com.auction.order_service.dto.OrderResponse;
import com.auction.order_service.dto.PurchaseRequest;
import com.auction.order_service.exception.BusinessException;
import com.auction.order_service.kafka.OrderConfirmation;
import com.auction.order_service.kafka.OrderProducer;
import com.auction.order_service.product.ProductClient;
import com.auction.order_service.repository.OrderRepository;
import com.auction.order_service.dto.OrderLineRequest;
import com.auction.order_service.service.orderLine.OrderLineService;
import com.auction.order_service.user.UserClient;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    //private final UserClient userClient;
    private final ProductClient productClient;
    private final OrderMapper orderMapper;
    private final OrderLineService orderLineService;
    private final OrderRepository orderRepository;
    private final OrderProducer orderProducer;

    public Long createOrder(String username, OrderRequest request) {
        //var buyer = this.userClient.findBuyerByUsername(request.userId())
        //.orElseThrow(() -> new BusinessException("Cannot create order:: No userId exists with id provided " , request.userId().toString()));

        var purchasedProducts = this.productClient.purchaseProducts(request.products());

        var order = this.orderRepository.save(orderMapper.toOrder(request));

       // var orderId = order.getOrderId();

        for (PurchaseRequest purchaseRequest : request.products()) {
            orderLineService.saveOrderLine(
                    new OrderLineRequest(
                            null,
                            //orderId,
                            purchaseRequest.productId(),
                            purchaseRequest.quantity()
                    )
            );
        }

        orderProducer.sendOrderOrderConfirmation(
                new OrderConfirmation(
                       request.reference(),
                        request.totalAmount(),
                        request.paymentMethod(),
                        username,
                        purchasedProducts
                )
        );

        //order.getOrderId
        return order.getOrderId();
    }

    public List<OrderResponse> findAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(orderMapper::fromOrder)
                .collect(Collectors.toList());
    }

    public OrderResponse findByOrderId(Long orderId) {
        return orderRepository.findById(orderId)
                .map(orderMapper::fromOrder)
                .orElseThrow(() -> new EntityNotFoundException(String.format("No order found with the order id provided: %d" , orderId)));
    }
}
