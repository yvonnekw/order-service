package com.auction.order_service.repository;

import com.auction.order_service.dto.OrderLineResponse;
import com.auction.order_service.orderLine.OrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {
    List<OrderLine> findAllByOrder_OrderId(Long orderId);
}
