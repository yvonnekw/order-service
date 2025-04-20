package com.auction.order_service.service;

import com.auction.order_service.client.PaymentServiceClient;
import com.auction.order_service.client.ProductServiceClient;
import com.auction.order_service.dto.*;
import com.auction.order_service.exception.BusinessException;
import com.auction.order_service.kafka.OrderConfirmation;
import com.auction.order_service.kafka.OrderProducer;
import com.auction.order_service.model.Order;
import com.auction.order_service.repository.OrderRepository;
import com.auction.order_service.service.orderLine.OrderLineService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderLineService orderLineService;
    private final OrderRepository orderRepository;
    private final OrderProducer orderProducer;
    private final PaymentServiceClient paymentServiceClient;
    private final ProductServiceClient productServiceClient;

    @Transactional
    public Long createOrder(String token, String username, String firstName, String lastName, String email, String idempotencyKey, OrderPaymentRequest request) {
        if (request == null || request.getPaymentRequest() == null || request.getOrderRequest() == null) {
            throw new IllegalArgumentException("OrderPaymentRequest, PaymentRequest, or OrderRequest cannot be null");
        }

        log.info("🧠 Idempotency Key: {}", idempotencyKey);

        String orderReference = generateOrderReference();
        log.info("📦 Creating order with reference: {}", orderReference);

        OrderRequest updatedOrderRequest = new OrderRequest(
                orderReference,
                request.getOrderRequest().totalAmount(),
                username,
                request.getOrderRequest().products()
        );

        List<PurchaseResponse> purchasedProducts;
        try {
            purchasedProducts = productServiceClient.purchaseProducts(idempotencyKey, updatedOrderRequest.products());
            log.info("✅ Purchased products: {}", purchasedProducts);
        } catch (Exception e) {
            log.error(" Failed to purchase products: {}", e.getMessage(), e);
            throw new BusinessException("Failed to purchase products: " + e.getMessage(), updatedOrderRequest.orderReference());
        }

        Order savedOrder = orderRepository.save(orderMapper.toOrder(updatedOrderRequest));
        orderRepository.flush();
        Long orderId = savedOrder.getOrderId();
        log.info(" Order persisted with ID: {}", orderId);

        PaymentRequest paymentRequest = new PaymentRequest(
                request.getOrderRequest().totalAmount(),
                request.getPaymentRequest().paymentMethod(),
                request.getPaymentRequest().isSuccessful()
        );
        log.info("PaymentRequest: {}", paymentRequest);

        PaymentResponse paymentResponse = paymentServiceClient.processPayment(
                token, username, firstName, lastName, email, idempotencyKey, request
        );

        if (!paymentResponse.isSuccessful()) {
            log.error("Payment failed for user: {}", username);
            throw new RuntimeException("Payment failed. Order cannot be completed.");
        }

        log.info("Payment successful for user: {}", username);

        Map<Long, PurchaseResponse> purchaseResponseMap = purchasedProducts.stream()
                .collect(Collectors.toMap(PurchaseResponse::productId, Function.identity(), (a, b) -> a));

        for (PurchaseRequest purchaseRequest : updatedOrderRequest.products()) {
            PurchaseResponse responseItem = purchaseResponseMap.get(purchaseRequest.productId());

            if (responseItem != null) {
                log.info("Saving OrderLine for productId: {}, enriched with PurchaseResponse", responseItem.productId());
                orderLineService.saveOrderLine(new OrderLineRequest(
                        orderId,
                        responseItem.productId(),
                        purchaseRequest.quantity(),
                        responseItem.productName(),
                        responseItem.brandName(),
                        responseItem.description(),
                        responseItem.colour(),
                        responseItem.productSize(),
                        responseItem.startingPrice(),
                        responseItem.buyNowPrice(),
                        responseItem.productImageUrl()
                ));
            } else {
                log.warn("No PurchaseResponse found for productId: {}. Falling back to request data.", purchaseRequest.productId());
                orderLineService.saveOrderLine(new OrderLineRequest(
                        orderId,
                        purchaseRequest.productId(),
                        purchaseRequest.quantity(),
                        purchaseRequest.productName(),
                        purchaseRequest.brandName(),
                        purchaseRequest.description(),
                        purchaseRequest.colour(),
                        purchaseRequest.productSize(),
                        purchaseRequest.startingPrice(),
                        purchaseRequest.buyNowPrice(),
                        purchaseRequest.productImageUrl()
                ));
            }
        }

        log.info("Order lines successfully saved for orderId: {}", orderId);

        orderProducer.sendOrderOrderConfirmation(new OrderConfirmation(
                orderReference,
                updatedOrderRequest.totalAmount(),
                username,
                firstName,
                lastName,
                email,
                purchasedProducts
        ));
        log.info("Order confirmation event sent for reference: {}", orderReference);

        return orderId;
    }

    @Transactional
    public Long processOrderWithPayment(String token, String username, String firstName, String lastName, String email, String idempotencyKey, OrderPaymentRequest request, Long orderId) {
        log.info("📥 Processing payment for user: {}", username);

        PaymentResponse paymentResponse = paymentServiceClient.processPayment(token, username, firstName, lastName, email, idempotencyKey, request);
        log.info("💳 Payment Service Response: {}", paymentResponse);

        if (!paymentResponse.isSuccessful()) {
            log.error("❌ Payment failed for user: {}", username);
            throw new RuntimeException("Payment failed, order cannot be processed.");
        }

        for (PurchaseRequest purchaseRequest : request.getOrderRequest().products()) {
            Long productId = purchaseRequest.productId();
            int quantity = purchaseRequest.quantity();

            try {
                ProductResponse product = productServiceClient.findProductById(productId);
                if (product != null) {
                    productServiceClient.updateProduct(new ProductResponse(
                            product.productId(),
                            quantity,
                            true,
                            false
                    ));
                    productServiceClient.markProductAsBought(productId);
                    log.info("Updated and marked product {} as bought", productId);
                }
            } catch (Exception e) {
                log.error("Error updating product ID {}: {}", productId, e.getMessage());
                throw new RuntimeException("Product update failed during order processing.");
            }
        }

        for (PurchaseRequest purchaseRequest : request.getOrderRequest().products()) {
            orderLineService.saveOrderLine(
                    new OrderLineRequest(orderId,
                            purchaseRequest.productId(),
                            purchaseRequest.quantity(),
                            purchaseRequest.productName(),
                            purchaseRequest.brandName(),
                            purchaseRequest.description(),
                            purchaseRequest.colour(),
                            purchaseRequest.productSize(),
                            purchaseRequest.startingPrice(),
                            purchaseRequest.buyNowPrice(),
                            purchaseRequest.productImageUrl())
            );
        }

        log.info("📨 Sending order confirmation...");
        orderProducer.sendOrderOrderConfirmation(new OrderConfirmation(
                request.getOrderRequest().orderReference(),
                request.getOrderRequest().totalAmount(),
                username,
                firstName,
                lastName,
                email,
                productServiceClient.purchaseProducts(idempotencyKey, request.getOrderRequest().products()) // Optional, if needed again
        ));

        log.info("Order successfully processed. Order ID: {}", orderId);
        return orderId;
    }

    @Transactional
    public List<OrderResponse> findAllOrders() {
        List<Order> orders = orderRepository.findAll();

        return orders.stream()
                .map(order -> {
                    List<OrderLineResponse> orderLineResponses = order.getOrderLines().stream()
                            .map(orderLine -> new OrderLineResponse(
                                    orderLine.getOrderLineId(),
                                    orderLine.getProductId(),
                                    orderLine.getProductName(),
                                    orderLine.getBrandName(),
                                    orderLine.getDescription(),
                                    orderLine.getColour(),
                                    orderLine.getProductSize(),
                                    orderLine.getStartingPrice(),
                                    orderLine.getBuyNowPrice(),
                                    orderLine.getQuantity(),
                                    orderLine.getProductImageUrl()
                            ))
                            .collect(Collectors.toList());

                    return new OrderResponse(
                            order.getOrderId(),
                            order.getOrderReference(),
                            order.getTotalAmount(),
                            order.getUsername(),
                            order.getCreatedDate(),
                            order.getLastModifiedDate(),
                            orderLineResponses
                    );
                })
                .collect(Collectors.toList());
    }


    @Transactional
    public OrderResponse findByOrderId(Long orderId) {
        return orderRepository.findById(orderId)
                .map(order -> {
                    List<OrderLineResponse> orderLineResponses = order.getOrderLines().stream()
                            .map(orderLine -> new OrderLineResponse(
                                    orderLine.getOrderLineId(),
                                    orderLine.getProductId(),
                                    orderLine.getProductName(),
                                    orderLine.getBrandName(),
                                    orderLine.getDescription(),
                                    orderLine.getColour(),
                                    orderLine.getProductSize(),
                                    orderLine.getStartingPrice(),
                                    orderLine.getBuyNowPrice(),
                                    orderLine.getQuantity(),
                                    orderLine.getProductImageUrl()
                            ))
                            .collect(Collectors.toList());

                    return new OrderResponse(
                            order.getOrderId(),
                            order.getOrderReference(),
                            order.getTotalAmount(),
                            order.getUsername(),
                            order.getCreatedDate(),
                            order.getLastModifiedDate(),
                            orderLineResponses
                    );
                })
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("No order found with the order id provided: %d", orderId)
                ));
    }

    public List<OrderResponse> findOrdersByUsername(String username) {
        List<Order> orders = orderRepository.findByUsername(username);

        return orders.stream()
                .map(order -> new OrderResponse(
                        order.getOrderId(),
                        order.getOrderReference(),
                        order.getTotalAmount(),
                        order.getUsername(),
                        order.getCreatedDate(),
                        order.getLastModifiedDate(),
                        order.getOrderLines().stream()
                                .map(orderLine -> new OrderLineResponse(
                                        orderLine.getOrderLineId(),
                                        orderLine.getProductId(),
                                        orderLine.getProductName(),
                                        orderLine.getBrandName(),
                                        orderLine.getDescription(),
                                        orderLine.getColour(),
                                        orderLine.getProductSize(),
                                        orderLine.getStartingPrice(),
                                        orderLine.getBuyNowPrice(),
                                        orderLine.getQuantity(),
                                        orderLine.getProductImageUrl()
                                )).toList()
                )).toList();
    }


    public static String generateOrderReference() {
        String date = new SimpleDateFormat("yyyyMMdd").format(new Date());

        String randomSegment = UUID.randomUUID().toString().substring(0, 8);

        return "ORDER-" + date + "-" + randomSegment;
    }
}