package com.auction.order_service.service;

import com.auction.order_service.client.PaymentServiceClient;
import com.auction.order_service.client.ProductServiceClient;
import com.auction.order_service.dto.*;
import com.auction.order_service.exception.BusinessException;
import com.auction.order_service.kafka.OrderConfirmation;
import com.auction.order_service.kafka.OrderProducer;
//import com.auction.order_service.product.ProductClient;
import com.auction.order_service.model.Order;
import com.auction.order_service.repository.OrderRepository;
import com.auction.order_service.service.orderLine.OrderLineService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
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

    public Long processOrderWithPayment(String username, String firstName, String lastName, String email, OrderPaymentRequest request, Long orderId) {

        log.info("Processing payment for user: {}", username);
        PaymentResponse paymentResponse = paymentServiceClient.processPayment(firstName, lastName, email, request);

        if (!paymentResponse.isSuccessful()) {
            log.error("Payment failed for user: {}", username);
            throw new RuntimeException("Payment failed, order cannot be processed.");
        }


        log.info("Marking products as purchased...");
        var purchasedProducts = productServiceClient.purchaseProducts(request.getOrderRequest().products());

        for (PurchaseRequest purchaseRequest : request.getOrderRequest().products()) {
            Long productId = purchaseRequest.productId();
            Integer quantity = purchaseRequest.quantity();

            try {

                productServiceClient.markProductAsBought(productId);

                ProductResponse productResponse = productServiceClient.findProductById(productId);
                if (productResponse != null && productResponse.quantity() <= 0) {
                    productServiceClient.updateProduct(new ProductResponse(
                            productResponse.productId(),
                            productResponse.username(),
                            productResponse.productName(),
                            productResponse.brandName(),
                            productResponse.description(),
                            productResponse.startingPrice(),
                            productResponse.buyNowPrice(),
                            productResponse.colour(),
                            productResponse.productSize(),
                            productResponse.quantity(),
                            false,
                            productResponse.isSold()
                    ));
                    log.info("Product {} marked as unavailable. Quantity: 0", productResponse.productName());
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
                        request.getOrderRequest().reference(),
                        request.getOrderRequest().totalAmount(),
                        request.getOrderRequest().paymentMethod(),
                        username,
                        purchasedProducts
                )
        );

        log.info("Order successfully processed. Order ID: {}", orderId);
        return orderId;
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
                .orElseThrow(() -> new EntityNotFoundException(String.format("No order found with the order id provided: %d", orderId)));
    }

    public Long createOrder(String username, String firstName, String lastName, String email, OrderPaymentRequest request) {
        if (request == null || request.getPaymentRequest() == null || request.getOrderRequest() == null) {
            throw new IllegalArgumentException("OrderPaymentRequest, PaymentRequest, or OrderRequest cannot be null");
        }

        log.info("Creating order for reference: {}", request.getOrderRequest().reference());

        String orderReference = generateOrderReference();
        OrderRequest updatedOrderRequest = new OrderRequest(
                orderReference,
                request.getOrderRequest().totalAmount(),
                request.getOrderRequest().paymentMethod(),
                request.getOrderRequest().products()
        );

        PaymentRequest paymentRequest = request.getPaymentRequest();

        List<PurchaseResponse> purchasedProducts;
        try {
            purchasedProducts = productServiceClient.purchaseProducts(updatedOrderRequest.products());
        } catch (Exception e) {
            throw new BusinessException("Failed to purchase products: " + e.getMessage(), paymentRequest.paymentMethod() );//sort this out
        }

        var order = orderRepository.save(orderMapper.toOrder(updatedOrderRequest));
        Long orderId = order.getOrderId();

        for (PurchaseRequest purchaseRequest : updatedOrderRequest.products()) {
            orderLineService.saveOrderLine(
                    new OrderLineRequest(orderId, purchaseRequest.productId(), purchaseRequest.quantity())
            );
        }

        orderProducer.sendOrderOrderConfirmation(
                new OrderConfirmation(
                        orderReference,
                        updatedOrderRequest.totalAmount(),
                        updatedOrderRequest.paymentMethod(),
                        username,
                        purchasedProducts
                )
        );

        processOrderWithPayment(username, firstName, lastName, email, request, orderId);

        return orderId;
    }

/*
    public Long createOrder(String username, String firstName, String lastName, String email, OrderPaymentRequest request) {

        log.info("request coming in {} {}", request.getOrderRequest(), request.getPaymentRequest());
        if (request.getOrderRequest() == null) {
            throw new IllegalArgumentException("OrderPaymentRequest or OrderRequest cannot be null");
        }

        // Generate a unique order reference
        String orderReference = generateOrderReference();

        // Create a new OrderRequest with the updated reference
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
                        orderRequest.reference(),
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
                        request.getOrderRequest().reference(),
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