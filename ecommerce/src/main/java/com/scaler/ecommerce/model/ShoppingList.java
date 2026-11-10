package com.scaler.ecommerce.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ShoppingList {
    @Id
    private Long userId;
    @Version
    private Long version;
    private UUID activeCheckoutId;
    @ElementCollection
    @MapKeyColumn(name = "product_id")
    private Map<Long, Integer> cart = new HashMap<>();
    @ElementCollection
    private Set<Long> wishlist = new HashSet<>();
}
