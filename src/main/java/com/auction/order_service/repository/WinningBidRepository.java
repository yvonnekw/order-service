package com.auction.order_service.repository;

import com.auction.order_service.model.WinningBid;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WinningBidRepository extends JpaRepository<WinningBid, Long> {
}
