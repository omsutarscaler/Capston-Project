package com.scaler.ecommorce.capston.service;

import com.scaler.ecommorce.capston.model.Category;

public interface CategoryService {

    public Category createCategory(String Name);

    Category findByName(String name);
}
