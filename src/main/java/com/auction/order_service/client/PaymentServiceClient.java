package com.auction.order_service.client;


import com.auction.order_service.dto.OrderPaymentRequest;
import com.auction.order_service.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceClient {

    private final RestTemplate restTemplate;

    @Value("${application.config.payment-url}")
    private String paymentServiceUrl;

    public PaymentResponse processPayment(@RequestHeader("Authorization") String token, String username, String firstName, String lastName, String email, String  idempotencyKey, OrderPaymentRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.set("X-Username", username);
        headers.set("X-FirstName", firstName);
        headers.set("X-LastName", lastName);
        headers.set("X-Email", email);
        headers.set("Idempotency-Key",  idempotencyKey);

        log.info("Payment client from order payment client - PaymentRequest : {} ", request);

        HttpEntity<OrderPaymentRequest> entity = new HttpEntity<>(request, headers);
        log.info("Processing payment for user in the payment client: {}, {}", username, entity.getBody());
        log.info("Raw Payment Service Response: {}", entity.getBody());

        return restTemplate.postForObject(paymentServiceUrl + "/process-payment", entity, PaymentResponse.class);
    }

}

