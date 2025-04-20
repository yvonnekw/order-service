package com.auction.order_service.controller;

import com.auction.order_service.dto.OrderPaymentRequest;
import com.auction.order_service.dto.OrderResponse;
import com.auction.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create-order")
    public ResponseEntity<Long> createOrder(@RequestHeader("Authorization") String token,
                                            @RequestHeader("X-Username") String username,
                                            @RequestHeader("X-FirstName") String firstName,
                                            @RequestHeader("X-LastName") String lastName,
                                            @RequestHeader("X-Email") String email,
                                            @RequestHeader("Idempotency-Key") String idempotencyKey,
                                            @RequestBody OrderPaymentRequest request) {

        if (request.getPaymentRequest() == null || request.getOrderRequest() == null) {
            throw new IllegalArgumentException("Both PaymentRequest and OrderRequest are required.");
        }

        log.info("Request received: PaymentRequest={}, OrderRequest={}",
                request.getPaymentRequest(), request.getOrderRequest());

        return ResponseEntity.ok(orderService.createOrder(token, username, firstName, lastName, email, idempotencyKey, request));
    }

    @GetMapping("/get-all-orders")
    public ResponseEntity<List<OrderResponse>> findAllOrders(@RequestHeader("Authorization") String token,  @RequestHeader("Idempotency-Key") String idempotencyKey) {
        List<OrderResponse> orders = orderService.findAllOrders();

        if (orders.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{order-id}")
    public ResponseEntity<OrderResponse> findByOrderId(@RequestHeader("Authorization") String token,
            @PathVariable("order-id") Long orderId) {
        return ResponseEntity.ok(orderService.findByOrderId(orderId));
    }

    @GetMapping("/username")
    public ResponseEntity<List<OrderResponse>> findOrdersByUsername(@RequestHeader("Authorization") String token,  @RequestHeader("X-Username") String username) {
        return ResponseEntity.ok(orderService.findOrdersByUsername(username));
    }

    @GetMapping()
    @ResponseStatus(HttpStatus.OK)
    public String getPayment() {
        return "order api is working ";
    }

}
