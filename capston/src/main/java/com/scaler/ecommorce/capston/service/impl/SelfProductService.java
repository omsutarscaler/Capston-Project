package com.scaler.ecommorce.capston.service.impl;

import com.scaler.ecommorce.capston.service.CategoryService;
import com.scaler.ecommorce.capston.service.ProductService;

import com.scaler.ecommorce.capston.exceptions.ProductNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaler.ecommorce.capston.model.Category;
import com.scaler.ecommorce.capston.model.Product;
import com.scaler.ecommorce.capston.dao.ProductRepository;

@Service
public class SelfProductService implements ProductService {
    private static final Sort PAGE_ORDER = Sort.by("price", "id");
    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    public SelfProductService(ProductRepository productRepository, CategoryService categoryService) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
    }

    @Override
    public Product getProductById(long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product " + id + " does not exist"));
    }

    @Override
    public Page<Product> getAllProducts(int page, int size) {
        return productRepository.findAll(PageRequest.of(page, size, PAGE_ORDER));
    }

    @Override
    @Transactional
    public Product createProduct(String title, String image, String description, double price, String categoryName) {
        Category category = categoryService.findByName(categoryName);
        if (category == null) {
            category = categoryService.createCategory(categoryName);
        }

        Product product = new Product();
        product.setDescription(description);
        product.setTitle(title);
        product.setPrice(price);
        product.setImage(image);
        product.setCategory(category);

        return productRepository.save(product);
    }

    @Override
    public Product updateProductPrice(long id, double price) {
        Product product = getProductById(id);
        product.setPrice(price);
        return productRepository.save(product);
    }

    @Override
    public Page<Product> findByProductTitle(String title, int page, int size) {
        return productRepository.findByTitle(title, PageRequest.of(page, size, PAGE_ORDER));
    }
}
