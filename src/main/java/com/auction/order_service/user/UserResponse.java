package com.auction.order_service.user;

public record UserResponse(
        String username,
        String firstName,
        String lastName,
        String email
) {
}
