package com.auction.order_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class WinningBid {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long winningBidId;
    private Long bidId;
    private Long productId;
    private String username;
    private BigDecimal bidAmount;
    private LocalDateTime bidTime;
    private Boolean paymentCompleted = false;
    private LocalDateTime paymentDate;
}
