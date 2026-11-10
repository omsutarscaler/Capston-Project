package com.scaler.ecommerce.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.Map;
import java.util.TreeMap;

import com.scaler.ecommerce.components.PaymentGatewayClient;
import com.scaler.ecommerce.components.PaymentGatewayClient.Payment;
import com.scaler.ecommerce.dao.CheckoutOrderRepository;
import com.scaler.ecommerce.model.CheckoutOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CheckoutService {
    private static final Duration RETRY_WINDOW = Duration.ofHours(23);
    private final CheckoutOrderRepository orders;
    private final ProductService products;
    private final PaymentGatewayClient gateway;
    private final JavaMailSender mail;
    private final TransactionTemplate transaction;
    private final String receiptFrom;
    private final InventoryService inventory;
    private final ShoppingService shopping;

    public CheckoutService(CheckoutOrderRepository orders, ProductService products, PaymentGatewayClient gateway,
                           JavaMailSender mail, TransactionTemplate transaction, InventoryService inventory,
                           ShoppingService shopping,
                           @Value("${checkout.receipt-from}") String receiptFrom) {
        this.orders = orders;
        this.products = products;
        this.gateway = gateway;
        this.mail = mail;
        this.transaction = transaction;
        this.receiptFrom = receiptFrom;
        this.inventory = inventory;
        this.shopping = shopping;
    }

    public Checkout start(UUID id, Long productId, long userId, String email) {
        long selectedProduct = productId == null ? 0 : productId;
        if (!orders.existsById(id)) {
            Map<Long, Integer> quantities = productId == null ? shopping.contents(userId).cart() : Map.of(productId, 1);
            if (quantities.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "The cart is empty");
            }
            long amount = 0;
            String title = "Ecommerce cart";
            for (var entry : quantities.entrySet()) {
                var product = products.getProductById(entry.getKey());
                double price = product.getPrice();
                if (!Double.isFinite(price) || price < 0.01 || price > 999_999.99) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Product price is outside checkout limits");
                }
                amount += BigDecimal.valueOf(price).movePointRight(2)
                        .setScale(0, RoundingMode.HALF_UP).longValueExact() * entry.getValue();
                if (productId != null) title = product.getTitle();
            }
            if (amount > 99_999_999) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Order amount is outside checkout limits");
            }
            var order = new CheckoutOrder();
            order.setId(id);
            order.setProductId(selectedProduct);
            order.getQuantities().putAll(quantities);
            order.setUserId(userId);
            order.setEmail(email);
            order.setTitle(title);
            order.setAmountMinor(amount);
            order.setCurrency("INR");
            order.setCreatedAt(Instant.now());
            try {
                orders.saveAndFlush(order);
            } catch (DataIntegrityViolationException e) {
                if (!orders.existsById(id)) {
                    throw e;
                }
            }
        }
        return transaction.execute(status -> {
            CheckoutOrder order = owned(locked(id), userId);
            if (order.getProductId() != selectedProduct) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Checkout ID already used for another product");
            }
            if (order.getPaymentId() == null) {
                if (order.getCreatedAt().plus(RETRY_WINDOW).isBefore(Instant.now())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Checkout retry window has closed");
                }
                new TreeMap<>(order.getQuantities()).forEach(inventory::reserve);
                if (order.getProductId() == 0) {
                    shopping.beginCheckout(userId, id, order.getQuantities());
                }
                Payment payment = gateway.create(order);
                verify(order, payment);
                if (payment.checkoutUrl() == null || !payment.checkoutUrl().startsWith("https://checkout.stripe.com/")) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment gateway returned an invalid checkout URL");
                }
                order.setPaymentId(payment.id());
                order.setCheckoutUrl(payment.checkoutUrl());
                order.setNextCheckAt(Instant.now().plusSeconds(30));
            }
            return Checkout.from(order);
        });
    }

    public Checkout confirm(UUID id, long userId) {
        return transaction.execute(status -> reconcile(owned(locked(id), userId), false));
    }

    public Checkout cancel(UUID id, long userId) {
        return transaction.execute(status -> reconcile(owned(locked(id), userId), true));
    }

    @Scheduled(fixedDelay = 5000)
    public void checkPendingPayments() {
        for (UUID id : orders.findDueChecks(Instant.now(), PageRequest.of(0, 1))) {
            try {
                transaction.executeWithoutResult(status -> reconcile(locked(id), false));
            } catch (ResponseStatusException e) {
                transaction.executeWithoutResult(status -> {
                    CheckoutOrder order = locked(id);
                    if (order.getNextCheckAt() != null) {
                        order.setNextCheckAt(Instant.now().plusSeconds(60));
                    }
                });
            }
        }
    }

    private Checkout reconcile(CheckoutOrder order, boolean cancel) {
        if (order.getPaymentId() == null || order.isExpired() || order.getReceiptSentAt() != null) {
            return Checkout.from(order);
        }
        if (!order.isPaid()) {
            Payment payment = cancel ? gateway.cancel(order.getPaymentId()) : gateway.get(order.getPaymentId());
            verify(order, payment);
            order.setPaid("paid".equals(payment.paymentStatus()));
            order.setExpired(!order.isPaid() && "expired".equals(payment.checkoutStatus()));
            if (order.isPaid() || order.isExpired()) {
                new TreeMap<>(order.getQuantities()).forEach((productId, quantity) ->
                        inventory.settle(productId, quantity, order.isPaid()));
                if (order.getProductId() == 0) {
                    shopping.finishCheckout(order.getUserId(), order.getId(), order.isPaid());
                }
            }
        }
        if (order.isPaid()) {
            sendReceipt(order);
        }
        order.setNextCheckAt(order.isExpired() || order.getReceiptSentAt() != null
                ? null : Instant.now().plusSeconds(30));
        return Checkout.from(order);
    }

    private void sendReceipt(CheckoutOrder order) {
        var receipt = new SimpleMailMessage();
        receipt.setFrom(receiptFrom);
        receipt.setTo(order.getEmail());
        receipt.setSubject("Ecommerce receipt " + order.getId());
        receipt.setText("Payment confirmed\nOrder: " + order.getId() + "\nProduct: " + order.getTitle()
                + "\nTotal: " + order.getCurrency() + " " + BigDecimal.valueOf(order.getAmountMinor(), 2)
                + "\nPayment reference: " + order.getPaymentId());
        try {
            mail.send(receipt);
            order.setReceiptSentAt(Instant.now());
        } catch (MailException e) {
            // Keep the receipt pending for the next reconciliation.
        }
    }

    private CheckoutOrder locked(UUID id) {
        return orders.lockById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Checkout not found"));
    }

    private static CheckoutOrder owned(CheckoutOrder order, long userId) {
        if (order.getUserId() != userId) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Checkout not found");
        }
        return order;
    }

    private static void verify(CheckoutOrder order, Payment payment) {
        if (!PaymentGatewayClient.SOURCE.equals(payment.source()) || !order.getId().toString().equals(payment.reference())
                || !Long.valueOf(order.getAmountMinor()).equals(payment.amountMinor())
                || !order.getCurrency().equalsIgnoreCase(payment.currency())
                || (order.getPaymentId() != null && !order.getPaymentId().equals(payment.id()))) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment details do not match the order");
        }
    }

    public record Checkout(UUID orderId, String checkoutUrl, boolean paid, boolean expired, boolean receiptSent) {
        static Checkout from(CheckoutOrder order) {
            return new Checkout(order.getId(), order.isPaid() || order.isExpired() ? null : order.getCheckoutUrl(),
                    order.isPaid(), order.isExpired(), order.getReceiptSentAt() != null);
        }
    }
}
