package com.auction.order_service.service;

import com.auction.order_service.client.PaymentServiceClient;
import com.auction.order_service.client.ProductServiceClient;
import com.auction.order_service.dto.*;
import com.auction.order_service.exception.BusinessException;
import com.auction.order_service.kafka.OrderConfirmation;
import com.auction.order_service.kafka.OrderProducer;
//import com.auction.order_service.product.ProductClient;
import com.auction.order_service.model.Order;
import com.auction.order_service.model.PaymentMethod;
import com.auction.order_service.repository.OrderRepository;
import com.auction.order_service.service.orderLine.OrderLineService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestHeader;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
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

    //private final UserClient userClient;
    //private final ProductClient productClient;
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

        // 🔑 Generate reference for the order
        String orderReference = generateOrderReference();
        log.info("📦 Creating order with reference: {}", orderReference);

        // 🔄 Build updated OrderRequest
        OrderRequest updatedOrderRequest = new OrderRequest(
                orderReference,
                request.getOrderRequest().totalAmount(),
                username,
                request.getOrderRequest().products()
        );

        // 📥 Attempt product purchase via product-service
        List<PurchaseResponse> purchasedProducts;
        try {
            purchasedProducts = productServiceClient.purchaseProducts(idempotencyKey, updatedOrderRequest.products());
            log.info("✅ Purchased products: {}", purchasedProducts);
        } catch (Exception e) {
            log.error("❌ Failed to purchase products: {}", e.getMessage(), e);
            throw new BusinessException("Failed to purchase products: " + e.getMessage(), updatedOrderRequest.orderReference());
        }

        // 🗃 Persist Order
        Order savedOrder = orderRepository.save(orderMapper.toOrder(updatedOrderRequest));
        orderRepository.flush(); // Ensure orderId is generated
        Long orderId = savedOrder.getOrderId();
        log.info("✅ Order persisted with ID: {}", orderId);

        // 💳 Process Payment
        PaymentRequest paymentRequest = new PaymentRequest(
                request.getOrderRequest().totalAmount(),
                request.getPaymentRequest().paymentMethod(),
                request.getPaymentRequest().isSuccessful()
        );
        log.info("💳 PaymentRequest: {}", paymentRequest);

        PaymentResponse paymentResponse = paymentServiceClient.processPayment(
                token, username, firstName, lastName, email, idempotencyKey, request
        );

        if (!paymentResponse.isSuccessful()) {
            log.error("❌ Payment failed for user: {}", username);
            throw new RuntimeException("Payment failed. Order cannot be completed.");
        }

        log.info("✅ Payment successful for user: {}", username);

        // 🧾 Save Order Lines using enriched PurchaseResponse data
        Map<Long, PurchaseResponse> purchaseResponseMap = purchasedProducts.stream()
                .collect(Collectors.toMap(PurchaseResponse::productId, Function.identity(), (a, b) -> a));

        for (PurchaseRequest purchaseRequest : updatedOrderRequest.products()) {
            PurchaseResponse responseItem = purchaseResponseMap.get(purchaseRequest.productId());

            if (responseItem != null) {
                log.info("Saving OrderLine for productId: {}, enriched with PurchaseResponse", responseItem.productId());
                orderLineService.saveOrderLine(new OrderLineRequest(
                        orderId,
                        responseItem.productId(),
                        purchaseRequest.quantity(), // Always respect quantity from original request
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
                // Fallback scenario (shouldn't happen if product-service is consistent)
                log.warn("⚠ No PurchaseResponse found for productId: {}. Falling back to request data.", purchaseRequest.productId());
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

        log.info("📄 Order lines successfully saved for orderId: {}", orderId);

        // 📤 Publish Order Confirmation Event
        orderProducer.sendOrderOrderConfirmation(new OrderConfirmation(
                orderReference,
                updatedOrderRequest.totalAmount(),
                username,
                firstName,
                lastName,
                email,
                purchasedProducts
        ));
        log.info("📬 Order confirmation event sent for reference: {}", orderReference);

        return orderId;
    }


    /*
    @Transactional
    public Long createOrder(String token, String username, String firstName, String lastName, String email, String idempotencyKey, OrderPaymentRequest request) {
        if (request == null || request.getPaymentRequest() == null || request.getOrderRequest() == null) {
            throw new IllegalArgumentException("OrderPaymentRequest, PaymentRequest, or OrderRequest cannot be null");
        }

        log.info("idempotencyKey ======= : {}", idempotencyKey);

        // 🔑 Generate reference for the order
        String orderReference = generateOrderReference();
        log.info("📦 Creating order for orderReference: {}", orderReference);

        // 🔄 Build updated OrderRequest
        OrderRequest updatedOrderRequest = new OrderRequest(
                orderReference,
                request.getOrderRequest().totalAmount(),
                username,
                request.getOrderRequest().products()
        );

        // 📥 Attempt product purchase via product-service
        List<PurchaseResponse> purchasedProducts;
        try {
            purchasedProducts = productServiceClient.purchaseProducts(idempotencyKey, updatedOrderRequest.products());
            log.info("✅ Purchased products from product-service: {}", purchasedProducts);
        } catch (Exception e) {
            log.error("❌ Product purchase failed: {}", e.getMessage());
            throw new BusinessException("Failed to purchase products: " + e.getMessage(), updatedOrderRequest.orderReference());
        }

        // 🗃 Save Order
        Order savedOrder = orderRepository.save(orderMapper.toOrder(updatedOrderRequest));
        orderRepository.flush(); // Force write for immediate ID
        Long orderId = savedOrder.getOrderId();
        log.info("✅ Order persisted with ID: {}", orderId);

        // 💳 Build the PaymentRequest
        PaymentRequest paymentRequest = new PaymentRequest(
                request.getOrderRequest().totalAmount(),
                request.getPaymentRequest().paymentMethod(),
                request.getPaymentRequest().isSuccessful()
        );
        log.info("PaymentRequest: {}", paymentRequest);

        // Call the payment service to process the payment
        PaymentResponse paymentResponse = paymentServiceClient.processPayment(
                token,
                username,
                firstName,
                lastName,
                email,
                idempotencyKey,
                request
        );

        if (!paymentResponse.isSuccessful()) {
            log.error("❌ Payment failed for user: {}", username);
            throw new RuntimeException("Payment failed, order cannot be processed.");
        }
        log.info("✅ Payment successfully processed for user: {}", username);

        // 🧾 Save Order Lines using PurchaseResponse for product details
        Map<Long, PurchaseResponse> purchaseResponseMap = purchasedProducts.stream()
                .collect(Collectors.toMap(PurchaseResponse::productId, Function.identity(), (a, b) -> a));

        for (PurchaseRequest purchaseRequest : updatedOrderRequest.products()) {
            PurchaseResponse responseItem = purchaseResponseMap.get(purchaseRequest.productId());

            if (responseItem != null) {
                log.info("Saving OrderLine for productId: {}, found response: {}", purchaseRequest.productId(), responseItem);
                orderLineService.saveOrderLine(new OrderLineRequest(
                        orderId,
                        responseItem.productId(),
                        purchaseRequest.quantity(), // Keep quantity from PurchaseRequest
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
                log.warn("⚠ No matching PurchaseResponse found for productId {} — orderLine will not have metadata", purchaseRequest.productId());
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

        log.info("📄 Order lines saved for orderId: {}", orderId);

        // 📤 Send Order Confirmation Event
        orderProducer.sendOrderOrderConfirmation(new OrderConfirmation(
                orderReference,
                updatedOrderRequest.totalAmount(),
                username,
                firstName,
                lastName,
                email,
                purchasedProducts
        ));
        log.info("📬 Order confirmation sent for orderReference: {}", orderReference);

        return orderId;
    }

*/

    /*
    @Transactional
    public Long createOrder(String token, String username, String firstName, String lastName, String email, String idempotencyKey, OrderPaymentRequest request) {

        if (request == null || request.getPaymentRequest() == null || request.getOrderRequest() == null) {
            throw new IllegalArgumentException("OrderPaymentRequest, PaymentRequest, or OrderRequest cannot be null");
        }

        log.info("idempotencyKey ======= : {}", idempotencyKey);

        // 🔑 Generate reference for the order
        String orderReference = generateOrderReference();
        log.info("📦 Creating order for orderReference: {}", orderReference);

        // 🔄 Build updated OrderRequest
        OrderRequest updatedOrderRequest = new OrderRequest(
                orderReference,
                request.getOrderRequest().totalAmount(),
                username,
                request.getOrderRequest().products()
        );

        // 📥 Attempt product purchase via product-service
        List<PurchaseResponse> purchasedProducts;
        try {
            purchasedProducts = productServiceClient.purchaseProducts(idempotencyKey, updatedOrderRequest.products() );
        } catch (Exception e) {
            log.error("❌ Product purchase failed: {}", e.getMessage());
            throw new BusinessException("Failed to purchase products: " + e.getMessage(), updatedOrderRequest.orderReference());
        }

        // 🗃 Save Order
        Order savedOrder = orderRepository.save(orderMapper.toOrder(updatedOrderRequest));
        orderRepository.flush(); // Force write for immediate ID
        Long orderId = savedOrder.getOrderId();
        log.info("✅ Order persisted with ID: {}", orderId);

        //look into this null
        // 💳 Build the PaymentRequest (now using the PaymentServiceClient)
        PaymentRequest paymentRequest = new PaymentRequest(
                request.getOrderRequest().totalAmount(),
                request.getPaymentRequest().paymentMethod(),
                request.getPaymentRequest().isSuccessful()
        );

        log.info("PaymentRequest: {}", paymentRequest);

        // Call the payment service to process the payment
        PaymentResponse paymentResponse = paymentServiceClient.processPayment(
                token,
                username,
                firstName,
                lastName,
                email,
                idempotencyKey,
                request
        );

        // Handle payment response
        if (!paymentResponse.isSuccessful()) {
            log.error("❌ Payment failed for user: {}", username);
            throw new RuntimeException("Payment failed, order cannot be processed.");
        }

        log.info("Payment successfully processed for user: {}", username);

        // 🧾 Save Order Lines
        for (PurchaseRequest purchaseRequest : updatedOrderRequest.products()) {
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
                    purchaseRequest.productImageUrl()));
        }
        log.info("📄 Order lines saved for orderId: {}", orderId);

        // 📤 Send Order Confirmation Event
        orderProducer.sendOrderOrderConfirmation(new OrderConfirmation(
                orderReference,
                updatedOrderRequest.totalAmount(),
                username,
                firstName,
                lastName,
                email,
                purchasedProducts
        ));

        log.info("📬 Order confirmation sent for orderReference: {}", orderReference);

        return orderId;
    }

*/
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
                    // ⚠ Make sure updateProduct accepts a complete/valid DTO
                    productServiceClient.updateProduct(new ProductResponse(
                            product.productId(),
                            quantity,
                            true, // isBought
                            false // isDeleted
                    ));
                    productServiceClient.markProductAsBought(productId);
                    log.info("✅ Updated and marked product {} as bought", productId);
                }
            } catch (Exception e) {
                log.error("⚠ Error updating product ID {}: {}", productId, e.getMessage());
                throw new RuntimeException("Product update failed during order processing.");
            }
        }

        // Save order lines only if not saved earlier
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

        //look into this
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

        log.info("✅ Order successfully processed. Order ID: {}", orderId);
        return orderId;
    }


    /*
    public Long processOrderWithPayment(@RequestHeader("Authorization") String token, String username, String firstName, String lastName, String email, OrderPaymentRequest request, Long orderId) {

        log.info("Processing payment for user: {}", username);
        PaymentResponse paymentResponse = paymentServiceClient.processPayment(token, username, firstName, lastName, email, request);
        log.info("Raw Payment Service Response: {}", paymentResponse);
        if (!paymentResponse.isSuccessful()) {
            log.error("Payment failed for user: {}", username);
            throw new RuntimeException("Payment failed, order cannot be processed.");
        }

        log.info("Marking products as purchased...");
        var purchasedProducts = productServiceClient.purchaseProducts(request.getOrderRequest().products());

        for (PurchaseRequest purchaseRequest : request.getOrderRequest().products()) {
            Long productId = purchaseRequest.productId();
            int quantity = purchaseRequest.quantity();

            try {


                ProductResponse productResponse = productServiceClient.findProductById(productId);
                if (productResponse != null && productResponse.quantity() <= 0) {
                    productServiceClient.markProductAsBought(productId);
                    productServiceClient.updateProduct(new ProductResponse(
                            productResponse.productId(),
                            //productResponse.username(),
                           // username,
                            //productResponse.productName(),
                            //productResponse.brandName(),
                           // productResponse.description(),
                           // productResponse.startingPrice(),
                            //productResponse.buyNowPrice(),
                           // productResponse.colour(),
                          //  productResponse.productSize(),
                            //productResponse.quantity(),
                            quantity,
                           true,
                            false

                    ));
                    log.info("Product {} marked as unavailable. Quantity: 0", productResponse.productId());
                }

            } catch (Exception e) {
                log.error("Error updating product ID: {}. Exception: {}", productId, e.getMessage());
                throw new RuntimeException("Product update failed during order processing.");
            }
        }

       // log.info("Creating order for user: {}", username);
        //var order = orderRepository.save(orderMapper.toOrder(request.orderId()));

        for (PurchaseRequest purchaseRequest : request.getOrderRequest().products()) {
            orderLineService.saveOrderLine(
                    new OrderLineRequest(
                            //null,
                            orderId,
                            purchaseRequest.productId(),
                            purchaseRequest.quantity()
                    )
            );
        }

        log.info("Sending order confirmation...");
        orderProducer.sendOrderOrderConfirmation(
                new OrderConfirmation(
                        request.getOrderRequest().orderReference(),
                        request.getOrderRequest().totalAmount(),
                        //request.getOrderRequest().paymentMethod(),
                        username,
                        firstName,
                        lastName,
                        email,
                        purchasedProducts
                )
        );

        log.info("Order successfully processed. Order ID: {}", orderId);
        return orderId;
    }
*/

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


    /*
@Transactional
    public List<OrderResponse> findAllOrders() {
        List<Order> orders = orderRepository.findAll();

        return orders.stream()
                .map(order -> {
                    List<PurchaseRequest> purchaseRequests = order.getOrderLines().stream()
                            .map(orderLine -> new PurchaseRequest(
                                    orderLine.getProductId(),
                                    orderLine.getQuantity(),
                                    orderLine.getProductName(),
                                    orderLine.getBrandName(),
                                    orderLine.getDescription(),
                                    orderLine.getColour(),
                                    orderLine.getProductSize(),
                                    orderLine.getStartingPrice(),
                                    orderLine.getBuyNowPrice(),
                                    orderLine.getProductImageUrl()))
                            .collect(Collectors.toList());
                    String idempotencyKey = UUID.randomUUID().toString();
                    //List<PurchaseResponse> purchasedProducts = productServiceClient.purchaseProducts(idempotencyKey, purchaseRequests);

                   // return orderMapper.fromOrder(order);
               // })
               // .collect(Collectors.toList());
    }
*/
/*
    public List<OrderResponse> findAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(order -> {
                    List<PurchaseResponse> purchasedProducts = productServiceClient.purchaseProducts(order.getOrderLines().stream()
                            .map(orderLine -> new PurchaseRequest(orderLine.getProductId(), orderLine.getQuantity()))
                            .collect(Collectors.toList()));
                    return orderMapper.fromOrder(order, purchasedProducts);
                })
                .collect(Collectors.toList());
    }
*/
/*
    public List<OrderResponse> findAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(orderMapper::fromOrder)
                .collect(Collectors.toList());
    }
    */
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


    /*
    public OrderResponse findByOrderId(Long orderId) {
        return orderRepository.findById(orderId)
                .map(order -> {
                    List<PurchaseResponse> purchasedProducts = productServiceClient.purchaseProducts(order.getOrderLines().stream()
                            .map(orderLine -> new PurchaseRequest(orderLine.getProductId(), orderLine.getQuantity()))
                            .collect(Collectors.toList()));
                    return orderMapper.fromOrder(order, purchasedProducts);
                })
                .orElseThrow(() -> new EntityNotFoundException(String.format("No order found with the order id provided: %d", orderId)));
    }

    */
/*
    public OrderResponse findByOrderId(Long orderId) {
        return orderRepository.findById(orderId)
                .map(orderMapper::fromOrder)
                .orElseThrow(() -> new EntityNotFoundException(String.format("No order found with the order id provided: %d", orderId)));
    }
*/
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


    /*
@Transactional
    public List<OrderResponse> findOrdersByUsername(String idempotencyKey, String username) {
        List<Order> orders = orderRepository.findByBuyer(username);

        return orders.stream()
                .map(order -> {
                    List<PurchaseRequest> purchaseRequests = order.getOrderLines().stream()
                            .map(orderLine -> new PurchaseRequest(
                                    orderLine.getProductId(),
                                    orderLine.getQuantity(),
                                    orderLine.getProductName(),
                                    orderLine.getBrandName(),
                                    orderLine.getDescription(),
                                    orderLine.getColour(),
                                    orderLine.getProductSize(),
                                    orderLine.getStartingPrice(),
                                    orderLine.getBuyNowPrice(),
                                    orderLine.getProductImageUrl()))
                            .toList();

                   // List<PurchaseResponse> purchasedProducts = productServiceClient.purchaseProducts(idempotencyKey, purchaseRequests);

                   // return orderMapper.fromOrder(order);
                //})
               // .collect(Collectors.toList());
    }
}
*/

    /*
    public List<OrderResponse> findOrdersByUsername(String username) {
        List<Order> orders = orderRepository.findByBuyer(username);
        return orders.stream()
                .map(order -> {
                    List<PurchaseResponse> purchasedProducts = productServiceClient.purchaseProducts(order.getOrderLines().stream()
                            .map(orderLine -> new PurchaseRequest(orderLine.getProductId(), orderLine.getQuantity()))
                            .collect(Collectors.toList()));
                    return orderMapper.fromOrder(order, purchasedProducts);
                })
                .collect(Collectors.toList());
    }
*/
    /*
    public List<OrderResponse> findOrdersByUsername(String username) {
        List<Order> orders = orderRepository.findByBuyer(username);
        return orders.stream()
                .map(orderMapper::fromOrder)
                .collect(Collectors.toList());
    }

    */
/*
    public Long createOrder(String token, String username, String firstName, String lastName, String email, OrderPaymentRequest request) {
        if (request == null || request.getPaymentRequest() == null || request.getOrderRequest() == null) {
            throw new IllegalArgumentException("OrderPaymentRequest, PaymentRequest, or OrderRequest cannot be null");
        }

        String orderReference = generateOrderReference();
        log.info("Creating order for orderReference: {}", orderReference);
        OrderRequest updatedOrderRequest = new OrderRequest(
                orderReference,
                request.getOrderRequest().totalAmount(),
               // request.getOrderRequest().paymentMethod(),
                username,
                request.getOrderRequest().products()
        );

        //PaymentRequest paymentRequest = request.getPaymentRequest();

        List<PurchaseResponse> purchasedProducts;
        try {
            purchasedProducts = productServiceClient.purchaseProducts(updatedOrderRequest.products());
        } catch (Exception e) {
            throw new BusinessException("Failed to purchase products: " + e.getMessage(), request.getOrderRequest().orderReference());//sort this out
        }

        var order = orderRepository.save(orderMapper.toOrder(updatedOrderRequest));
        Long orderId = order.getOrderId();

        PaymentRequest paymentRequest = new PaymentRequest(
                //orderId,
                //orderReference,
                request.getOrderRequest().totalAmount(),
                request.getPaymentRequest().paymentMethod(),
        request.getPaymentRequest().isSuccessful()
        );

        log.info("paymentRequest info: {}", paymentRequest);

        for (PurchaseRequest purchaseRequest : updatedOrderRequest.products()) {
            orderLineService.saveOrderLine(
                    new OrderLineRequest(orderId, purchaseRequest.productId(), purchaseRequest.quantity())
            );
        }

        orderProducer.sendOrderOrderConfirmation(
                new OrderConfirmation(
                        orderReference,
                        updatedOrderRequest.totalAmount(),
                       // updatedOrderRequest.paymentMethod(),
                        username,
                        firstName,
                        lastName,
                        email,
                        purchasedProducts
                )
        );

        OrderPaymentRequest newRequest = new OrderPaymentRequest(paymentRequest, updatedOrderRequest);
        log.info("new object OrderPaymentRequest info: {}", newRequest);


        processOrderWithPayment(token, username, firstName, lastName, email, newRequest, orderId);

        return orderId;
    }
*/
/*
    public Long createOrder(String username, String firstName, String lastName, String email, OrderPaymentRequest request) {

        log.info("request coming in {} {}", request.getOrderRequest(), request.getPaymentRequest());
        if (request.getOrderRequest() == null) {
            throw new IllegalArgumentException("OrderPaymentRequest or OrderRequest cannot be null");
        }

        // Generate a unique order orderReference
        String orderReference = generateOrderReference();

        // Create a new OrderRequest with the updated orderReference
        OrderRequest updatedOrderRequest = new OrderRequest(
                orderReference,
                request.getOrderRequest().totalAmount(),
                request.getOrderRequest().paymentMethod(),
                request.getOrderRequest().products()
        );

        var orderRequest = request.getOrderRequest();
        if (orderRequest.products() == null || orderRequest.products().isEmpty()) {
            throw new IllegalArgumentException("OrderRequest must contain at least one product");
        }
     //   var purchasedProducts;

        List<PurchaseResponse> purchasedProducts;
        try {
          purchasedProducts = this.productServiceClient.purchaseProducts(updatedOrderRequest.products());
            //purchasedProducts = this.productServiceClient.purchaseProducts(orderRequest.products());
        } catch (Exception e) {
            throw new BusinessException("Failed to purchase products: " , e.getMessage());
        }
        var order = this.orderRepository.save(orderMapper.toOrder(updatedOrderRequest));


        //var order = this.orderRepository.save(orderMapper.toOrder(orderRequest));
        var orderId = order.getOrderId();

        for (PurchaseRequest purchaseRequest : orderRequest.products()) {
            orderLineService.saveOrderLine(
                    new OrderLineRequest(

                            orderId,
                            purchaseRequest.productId(),
                            purchaseRequest.quantity()
                    )
            );
        }

        orderProducer.sendOrderOrderConfirmation(
                new OrderConfirmation(
                        orderRequest.orderReference(),
                        orderRequest.totalAmount(),
                        orderRequest.paymentMethod(),
                        username,
                        purchasedProducts
                )
        );

        processOrderWithPayment(username, firstName, lastName, email, request, orderId);

        return orderId;
    }

*/
/*
    public Long createOrder(String username, String firstName, String lastName, String email, OrderPaymentRequest request) {
        //var buyer = this.userClient.findBuyerByUsername(request.userId())
        //.orElseThrow(() -> new BusinessException("Cannot create order:: No userId exists with id provided " , request.userId().toString()));

        var purchasedProducts = this.productServiceClient.purchaseProducts(request.getOrderRequest().products());

        var order = this.orderRepository.save(orderMapper.toOrder(request.getOrderRequest()));

        // var orderId = order.getOrderId();

        for (PurchaseRequest purchaseRequest : request.getOrderRequest().products()) {
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
                        request.getOrderRequest().orderReference(),
                        request.getOrderRequest().totalAmount(),
                        request.getOrderRequest().paymentMethod(),
                        username,
                        purchasedProducts
                )
        );

        processOrderWithPayment(username, firstName, lastName, email, request, order.getOrderId());

        //order.getOrderId
        return order.getOrderId();
    }

    */

    public static String generateOrderReference() {
        String date = new SimpleDateFormat("yyyyMMdd").format(new Date());

        String randomSegment = UUID.randomUUID().toString().substring(0, 8);

        return "ORDER-" + date + "-" + randomSegment;
    }
}