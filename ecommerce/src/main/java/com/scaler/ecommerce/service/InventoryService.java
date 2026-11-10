package com.scaler.ecommerce.service;

import com.scaler.ecommerce.dao.InventoryRepository;
import com.scaler.ecommerce.dao.ProductRepository;
import com.scaler.ecommerce.exceptions.ProductNotFoundException;
import com.scaler.ecommerce.model.InventoryItem;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InventoryService {
    private final InventoryRepository inventory;
    private final ProductRepository products;

    public InventoryService(InventoryRepository inventory, ProductRepository products) {
        this.inventory = inventory;
        this.products = products;
    }

    public int available(long id) {
        return inventory.findById(id).map(InventoryItem::getAvailable).orElse(0);
    }

    @Transactional
    public int setAvailable(long id, int quantity) {
        products.lockById(id).orElseThrow(() -> new ProductNotFoundException("Product " + id + " does not exist"));
        InventoryItem item = inventory.lockById(id).orElseGet(() -> {
            var fresh = new InventoryItem();
            fresh.setProductId(id);
            return fresh;
        });
        item.setAvailable(quantity);
        return inventory.save(item).getAvailable();
    }

    public void reserve(long id, int quantity) {
        var item = inventory.lockById(id).orElseThrow(InventoryService::outOfStock);
        if (item.getAvailable() < quantity) throw outOfStock();
        item.setAvailable(item.getAvailable() - quantity);
        item.setReserved(item.getReserved() + quantity);
    }

    public void settle(long id, int quantity, boolean paid) {
        var item = inventory.lockById(id).orElseThrow();
        item.setReserved(item.getReserved() - quantity);
        if (!paid) item.setAvailable(item.getAvailable() + quantity);
    }

    private static ResponseStatusException outOfStock() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock");
    }
}
