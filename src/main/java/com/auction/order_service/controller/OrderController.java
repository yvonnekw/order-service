package com.auction.order_service.controller;

import com.auction.order_service.dto.OrderPaymentRequest;
import com.auction.order_service.dto.OrderRequest;
import com.auction.order_service.dto.OrderResponse;
import com.auction.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create-order")
    public ResponseEntity<Long> createOrder(@RequestHeader("X-Username") String username,
                                            @RequestHeader("X-FirstName") String firstName,
                                            @RequestHeader("X-LastName") String lastName,
                                            @RequestHeader("X-Email") String email,
                                            @RequestBody @Valid OrderPaymentRequest request) {

        if (request.getPaymentRequest() == null || request.getOrderRequest() == null) {
            throw new IllegalArgumentException("Both PaymentRequest and OrderRequest are required.");
        }

        log.info("Request received: PaymentRequest={}, OrderRequest={}",
                request.getPaymentRequest(), request.getOrderRequest());

        return ResponseEntity.ok(orderService.createOrder(username, firstName, lastName, email, request));
    }

/*
    @PostMapping("/create-order")
    public ResponseEntity<Long> createOrder(
            @RequestHeader("X-Username") String username,
            @RequestHeader("X-FirstName") String firstName,
            @RequestHeader("X-LastName") String lastName,
            @RequestHeader("X-Email") String email,
            @RequestBody @Valid OrderPaymentRequest request) {

        try {
            Long orderId = orderService.processOrderWithPayment(username, firstName, lastName, email, request);
            return ResponseEntity.ok(orderId);
        } catch (Exception e) {
            log.error("Error processing order for user: {}. Details: {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
*/

    /*
    @PostMapping("/create-order")
    public ResponseEntity<Long> createOrder(@RequestHeader("X-Username") String username, @RequestHeader("X-FirstName") String firstName, @RequestHeader("X-LastName") String lastName, @RequestHeader("X-Email") String email, @RequestBody @Valid PaymentRequest paymentRequest, OrderRequest orderRequest ) {

        return ResponseEntity.ok(orderService.processOrderWithPayment(username,  paymentRequest, orderRequest));
    }
*/
/*
    @PostMapping("/create-order")
    public ResponseEntity<Long> createOrder(@RequestHeader("X-Username") String username,
                                            @RequestHeader("X-FirstName") String firstName,
                                            @RequestHeader("X-LastName") String lastName,
                                            @RequestHeader("X-Email") String email,
                                            @RequestBody @Valid OrderPaymentRequest request) {

        log.info("request coming in {} {}", request.getOrderRequest(), request.getPaymentRequest());

        return ResponseEntity.ok(orderService.createOrder(username, firstName, lastName, email, request));
    }
*/
    @GetMapping("/get-all-orders")
    public ResponseEntity<List<OrderResponse>> findAllOrders() {
        List<OrderResponse> orders = orderService.findAllOrders();

        if (orders.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{order-id}")
    public ResponseEntity<OrderResponse> findByOrderId(
            @PathVariable("order-id") Long orderId) {
        return ResponseEntity.ok(orderService.findByOrderId(orderId));
    }

    @GetMapping()
    @ResponseStatus(HttpStatus.OK)
    public String getPayment() {
        return "order api is working ";
    }

}
