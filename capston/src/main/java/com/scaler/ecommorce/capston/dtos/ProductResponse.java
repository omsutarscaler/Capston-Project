package com.scaler.ecommorce.capston.dtos;

import com.scaler.ecommorce.capston.model.Product;

public record ProductResponse(long id, String title, String image, String description, double price,
                              CategoryResponse category) {
    public static ProductResponse from(Product product) {
        var category = product.getCategory();
        return new ProductResponse(product.getId(), product.getTitle(), product.getImage(),
                product.getDescription(), product.getPrice(),
                category == null ? null : new CategoryResponse(category.getId(), category.getName()));
    }

    public record CategoryResponse(Long id, String name) {}
}
