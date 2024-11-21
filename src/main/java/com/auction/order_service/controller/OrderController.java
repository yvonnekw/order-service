package com.auction.order_service.controller;

import com.auction.order_service.dto.OrderRequest;
import com.auction.order_service.dto.OrderResponse;
import com.auction.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;



    @PostMapping("/create-order")
    public ResponseEntity<Long> createOrder(@RequestHeader("X-Username") String username, @RequestBody @Valid OrderRequest request) {

        return ResponseEntity.ok(orderService.createOrder(username, request));
    }
    /*
    PostMapping("/create-order")
    public ResponseEntity<Long> createOrder(@RequestBody @Valid OrderRequest request) {

        return ResponseEntity.ok(orderService.createOrder(request));
    }*/

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
