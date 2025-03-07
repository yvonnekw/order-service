package com.auction.order_service.client;


import com.auction.order_service.dto.ProductResponse;
import com.auction.order_service.dto.PurchaseRequest;
import com.auction.order_service.dto.PurchaseResponse;
import com.auction.order_service.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

@Service
@RequiredArgsConstructor
public class ProductServiceClient {

    private final RestTemplate restTemplate;

    @Value("${application.config.product-url}")
    private String productServiceUrl;

    public void markProductAsBought(Long productId) {
        String url = productServiceUrl + "/{productId}/mark-as-bought";
        restTemplate.postForObject(url, null, Void.class, productId);
    }

    public ProductResponse findProductById(Long productId) {
        String url = productServiceUrl + "/{productId}";
        return restTemplate.getForObject(url, ProductResponse.class, productId);
    }

    // Method to update the product details after checkout
    public void updateProduct(ProductResponse productResponse) {
        String url = productServiceUrl + "/{productId}";
        restTemplate.put(url, productResponse, productResponse.productId());
    }

    public List<PurchaseResponse> purchaseProducts(List<PurchaseRequest> requestBody) {

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE);

        HttpEntity<List<PurchaseRequest>> requestEntity = new HttpEntity<>(requestBody, httpHeaders);

        ParameterizedTypeReference<List<PurchaseResponse>> responseType =
                new ParameterizedTypeReference<>() {};

        ResponseEntity<List<PurchaseResponse>> responseEntity = restTemplate.exchange(
                productServiceUrl + "/purchase",
                POST,
                requestEntity,
                responseType
        );

        if (responseEntity.getStatusCode().isError()) {
            throw new BusinessException("An error occurred while processing the product purchase: " , responseEntity.getStatusCode().toString());
        }
        return responseEntity.getBody();
    }
}
