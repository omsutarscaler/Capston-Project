package com.scaler.ecommorce.capston.dao;

import com.scaler.ecommorce.capston.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByTitle(String title, Pageable pageable);
}
