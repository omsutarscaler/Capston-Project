package com.scaler.ecommerce.service.impl;

import com.scaler.ecommerce.service.CategoryService;
import com.scaler.ecommerce.service.ProductService;

import com.scaler.ecommerce.exceptions.ProductNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaler.ecommerce.model.Category;
import com.scaler.ecommerce.model.Product;
import com.scaler.ecommerce.dao.ProductRepository;

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
    public Page<Product> browse(Long category, String search, String sort, int page, int size) {
        Sort order = switch (sort) {
            case "price-desc" -> Sort.by(Sort.Order.desc("price"), Sort.Order.asc("id"));
            case "newest" -> Sort.by(Sort.Order.desc("id"));
            default -> PAGE_ORDER;
        };
        return productRepository.browse(category, search.strip(), PageRequest.of(page, size, order));
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
