package com.scaler.ecommorce.capston.dtos;

import java.util.List;

import com.scaler.ecommorce.capston.model.Product;
import org.springframework.data.domain.Page;

public record ProductPageResponse(List<ProductResponse> content, int page, int size, long totalElements,
                                  int totalPages, boolean hasNext) {
    public static ProductPageResponse from(Page<Product> products) {
        return new ProductPageResponse(products.map(ProductResponse::from).getContent(), products.getNumber(),
                products.getSize(), products.getTotalElements(), products.getTotalPages(), products.hasNext());
    }
}
