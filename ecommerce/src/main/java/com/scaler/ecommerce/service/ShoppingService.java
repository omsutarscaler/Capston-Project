package com.scaler.ecommerce.service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.scaler.ecommerce.dao.ShoppingListRepository;
import com.scaler.ecommerce.model.ShoppingList;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ShoppingService {
    public static final int MAX_CART_ITEMS = 10;
    private final ShoppingListRepository lists;
    private final InventoryService inventory;
    private final ProductService products;
    private final TransactionTemplate transaction;

    public ShoppingService(ShoppingListRepository lists, InventoryService inventory, ProductService products,
                           TransactionTemplate transaction) {
        this.lists = lists;
        this.inventory = inventory;
        this.products = products;
        this.transaction = transaction;
    }

    @Transactional(readOnly = true)
    public Contents contents(long userId) {
        return lists.findById(userId).map(Contents::from).orElse(new Contents(Map.of(), Set.of(), null));
    }

    public Contents setQuantity(long userId, long productId, int quantity) {
        if (quantity > 0) products.getProductById(productId);
        ensureList(userId);
        return transaction.execute(status -> {
            var list = lists.lockById(userId).orElseThrow();
            if (list.getActiveCheckoutId() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "The cart has an active checkout");
            }
            var cart = list.getCart();
            int total = cart.values().stream().mapToInt(Integer::intValue).sum()
                    - cart.getOrDefault(productId, 0) + quantity;
            if (total > MAX_CART_ITEMS || (quantity > 0 && quantity > inventory.available(productId))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Quantity exceeds stock or the 10-item cart limit");
            }
            if (quantity == 0) cart.remove(productId);
            else cart.put(productId, quantity);
            return Contents.from(list);
        });
    }

    public Contents setSaved(long userId, long productId, boolean saved) {
        if (saved) products.getProductById(productId);
        ensureList(userId);
        return transaction.execute(status -> {
            var list = lists.lockById(userId).orElseThrow();
            if (saved) list.getWishlist().add(productId);
            else list.getWishlist().remove(productId);
            return Contents.from(list);
        });
    }

    public void beginCheckout(long userId, UUID orderId, Map<Long, Integer> snapshot) {
        var list = lists.lockById(userId).orElseThrow();
        if (list.getActiveCheckoutId() != null || !list.getCart().equals(snapshot)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The cart changed or already has an active checkout");
        }
        list.setActiveCheckoutId(orderId);
    }

    public void finishCheckout(long userId, UUID orderId, boolean paid) {
        lists.lockById(userId).ifPresent(list -> {
            if (orderId.equals(list.getActiveCheckoutId())) {
                if (paid) list.getCart().clear();
                list.setActiveCheckoutId(null);
            }
        });
    }

    private void ensureList(long userId) {
        if (lists.existsById(userId)) return;
        var list = new ShoppingList();
        list.setUserId(userId);
        try {
            lists.saveAndFlush(list);
        } catch (DataIntegrityViolationException e) {
            if (!lists.existsById(userId)) throw e;
        }
    }

    public record Contents(Map<Long, Integer> cart, Set<Long> wishlist, UUID activeCheckoutId) {
        static Contents from(ShoppingList list) {
            return new Contents(Map.copyOf(list.getCart()), Set.copyOf(list.getWishlist()), list.getActiveCheckoutId());
        }
    }
}
