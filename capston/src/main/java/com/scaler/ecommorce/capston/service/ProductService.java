package com.scaler.ecommorce.capston.service;

import org.springframework.data.domain.Page;

import com.scaler.ecommorce.capston.model.Product;

public interface ProductService {
    Product getProductById(long id);

    Page<Product> getAllProducts(int page, int size);

    Product createProduct(String title, String image, String description, double price, String categoryName);

    Product updateProductPrice(long id, double price);

    Page<Product> findByProductTitle(String title, int page, int size);

}
