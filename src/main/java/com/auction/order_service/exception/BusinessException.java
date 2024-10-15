package com.auction.order_service.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BusinessException extends RuntimeException {
    private final String code;
    private final String msg;


    public BusinessException(String code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }


    public boolean isProductUnavailable() {
        return "PRODUCT_UNAVAILABLE".equals(code);
    }


    public boolean isOrderNotFound() {
        return "ORDER_NOT_FOUND".equals(code);
    }
}
