package com.auction.order_service.client;


import com.auction.order_service.dto.ProductResponse;
import com.auction.order_service.dto.PurchaseRequest;
import com.auction.order_service.dto.PurchaseResponse;
import com.auction.order_service.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

@Service
@Slf4j
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
        httpHeaders.set(HttpHeaders.CONTENT_TYPE, "application/json");

        HttpEntity<List<PurchaseRequest>> requestEntity = new HttpEntity<>(requestBody, httpHeaders);

        ParameterizedTypeReference<List<PurchaseResponse>> responseType =
                new ParameterizedTypeReference<>() {};

        try {
            log.info("Sending purchase request to ProductService: {}", requestBody);
            ResponseEntity<List<PurchaseResponse>> responseEntity = restTemplate.exchange(
                    productServiceUrl + "/purchase",
                    HttpMethod.POST,
                    requestEntity,
                    responseType
            );

            if (responseEntity.getStatusCode().isError()) {
                log.error("Error processing the product purchase: {}", responseEntity.getStatusCode());
                throw new BusinessException("ERROR_PROCESSING_PURCHASE", "An error occurred while processing the product purchase: " + responseEntity.getStatusCode());
            }

            List<PurchaseResponse> responseBody = responseEntity.getBody();
            if (responseBody == null) {
                log.error("The response body is null after processing the product purchase.");
                throw new BusinessException("NULL_RESPONSE_BODY", "The response body is null after processing the product purchase.");
            }

            log.info("Received purchase response from ProductService: {}", responseBody);
            return responseBody;
        } catch (Exception e) {
            log.error("Exception occurred while processing the product purchase: {}", e.getMessage());
            throw new BusinessException("EXCEPTION_OCCURRED", "An exception occurred while processing the product purchase: " + e.getMessage());
        }
    }



/*
    //broken method
    public List<PurchaseResponse> purchaseProducts(List<PurchaseRequest> requestBody) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.CONTENT_TYPE, "application/json");

        HttpEntity<List<PurchaseRequest>> requestEntity = new HttpEntity<>(requestBody, httpHeaders);

        ParameterizedTypeReference<List<PurchaseResponse>> responseType =
                new ParameterizedTypeReference<>() {};

        try {
            ResponseEntity<List<PurchaseResponse>> responseEntity = restTemplate.exchange(
                    productServiceUrl + "/purchase",
                    HttpMethod.POST,
                    requestEntity,
                    responseType
            );

            if (responseEntity.getStatusCode().isError()) {
                log.error("Error processing the product purchase: {}", responseEntity.getStatusCode());
                throw new BusinessException("An error occurred while processing the product purchase:",  responseEntity.getStatusCode().toString());
            }

            List<PurchaseResponse> responseBody = responseEntity.getBody();
            if (responseBody == null) {
                log.error("The response body is null after processing the product purchase.");
                throw new BusinessException("The response body is null after processing the product purchase.", responseBody.toString());
            }

            return responseBody;
        } catch (Exception e) {
            log.error("Exception occurred while processing the product purchase: {}", e.getMessage());
            throw new BusinessException("An exception occurred while processing the product purchase: " + e.getMessage(), e.toString());
        }

    }
    */
/*
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

*/
}
