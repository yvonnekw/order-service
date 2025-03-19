package com.auction.order_service.repository;

import com.auction.order_service.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
   // List<Order> findByBuyer(String buyer);

    List<Order> findByUsername(String username);
}
