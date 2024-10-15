package com.auction.order_service.user;

public record UserResponse(
        Long userId,
        String firstName,
        String lastName,
        String email
) {
}
