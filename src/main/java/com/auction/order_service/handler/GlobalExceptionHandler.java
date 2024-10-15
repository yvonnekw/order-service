package com.auction.order_service.handler;

import com.auction.order_service.error.ErrorResponse;
import com.auction.order_service.exception.BusinessException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBusinessException(BusinessException ex) {

        if (ex.isProductUnavailable()) {
            return new ErrorResponse("PRODUCT_UNAVAILABLE", ex.getMessage());
        } else if (ex.isOrderNotFound()) {
            return new ErrorResponse("ORDER_NOT_FOUND", ex.getMessage());
        }

        return new ErrorResponse("UNKNOWN_ERROR", "An unknown error occurred");
    }
/*
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBad(BusinessException ex) {
        return new ErrorResponse("PRODUCT_UNAVAILABLE", ex.getMessage());
    }
    */
/*


    @ExceptionHandler(InsufficientFundsException.class)
    @ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
    public ErrorResponse handleInsufficientFunds(InsufficientFundsException ex) {
        return new ErrorResponse("INSUFFICIENT_FUNDS", ex.getMessage());
    }
*/

    /*
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handle(BusinessException ex) {
        return new ErrorResponse("ORDER_NOT_FOUND", ex.getMsg());
    }
*/

}







