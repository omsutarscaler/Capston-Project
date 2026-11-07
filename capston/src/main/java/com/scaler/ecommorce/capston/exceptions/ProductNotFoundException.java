package com.scaler.ecommorce.capston.exceptions;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String message){
        super(message);
    }
}
