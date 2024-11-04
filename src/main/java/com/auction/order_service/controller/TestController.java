package com.auction.order_service.controller;

import com.auction.order_service.model.Order;
import com.auction.order_service.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test-api/orders")
@Profile("test")
public class TestController {

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return ResponseEntity.of(orderRepository.findById(id));
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> resetDatabase() {
        orderRepository.deleteAll();
        return ResponseEntity.ok().build();
    }
}
