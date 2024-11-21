package com.auction.order_service.user;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

//@FeignClient(name = "user-service", url = "http://localhost:8585")
//@FeignClient(
        //name = "user-service",
        //url = "${application.config.user-url}"
//)
public interface UserClient {
   // @GetMapping("/{userId}")
   // @GetMapping("/api/v1/users/{userId}")
    //Optional<UserResponse> findBuyerById(@PathVariable("userId") Long userId);

}
