package com.auction.order_service.controller;

import com.auction.order_service.dto.OrderLineResponse;
import com.auction.order_service.service.orderLine.OrderLineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/order-lines")
@RequiredArgsConstructor
public class OrderLineController {
    private final OrderLineService orderLineService;

    @GetMapping("/order/{order-id}")
    public ResponseEntity<List<OrderLineResponse>> findAllByOrderId(@RequestHeader("Authorization") String token, @PathVariable("order-id") Long orderId) {
        return ResponseEntity.ok(orderLineService.findAllByOrderId(orderId));
    }
}
