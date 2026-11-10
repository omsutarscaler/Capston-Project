package com.scaler.ecommerce.controller;

import com.scaler.ecommerce.service.InventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
public class InventoryController {
    private final InventoryService inventory;

    public InventoryController(InventoryService inventory) {
        this.inventory = inventory;
    }

    @GetMapping("/{id}")
    public Stock get(@PathVariable long id) {
        return new Stock(inventory.available(id));
    }

    @PutMapping("/{id}")
    public Stock update(@PathVariable long id, @Valid @RequestBody Stock request) {
        return new Stock(inventory.setAvailable(id, request.available()));
    }

    public record Stock(@NotNull @Min(0) @Max(1_000_000) Integer available) {}
}
