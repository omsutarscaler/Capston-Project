package com.scaler.ecommerce.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class InventoryItem {
    @Id
    private Long productId;
    private int available;
    private int reserved;
}
