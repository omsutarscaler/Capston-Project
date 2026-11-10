package com.scaler.ecommerce.service;

import com.scaler.ecommerce.model.Category;

public interface CategoryService {

    public Category createCategory(String Name);

    Category findByName(String name);
}
