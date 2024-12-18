package com.auction.order_service.orderLine;

import com.auction.order_service.model.Order;
import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
@Entity
public class OrderLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderLineId;
    @ManyToOne
    @JoinColumn(name = "order_orderId")
    private Order order;
    private Long productId;
    private double quantity;
}
