package com.scaler.ecommorce.capston.service.impl;

import com.scaler.ecommorce.capston.service.CategoryService;

import com.scaler.ecommorce.capston.model.Category;
import com.scaler.ecommorce.capston.dao.CategoryRepository;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Category createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        return categoryRepository.save(category);
    }

    @Override
    public Category findByName(String name) {
        return categoryRepository.findByName(name);
    }
}
