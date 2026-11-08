package com.inventorymonitor.service;

import com.inventorymonitor.event.AppEvent;
import com.inventorymonitor.event.EventBus;
import com.inventorymonitor.event.Events;
import com.inventorymonitor.model.Product;
import com.inventorymonitor.model.StockMovement;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class InventoryService {
    private final StorageService storage;
    private final EventBus bus;
    private List<Product> products;
    private List<StockMovement> movements;

    public InventoryService(StorageService storage, EventBus bus) throws IOException {
        this.storage = storage;
        this.bus = bus;
        StorageService.Data data = storage.load();
        products = new ArrayList<>(data.products());
        movements = new ArrayList<>(data.movements());
        // Event chaining: a saved stock change can cause one warning event.
        bus.addListener(this::onEvent);
    }

    private void onEvent(AppEvent event) {
        if (!(event instanceof Events.StockChanged change)) {
            return;
        }
        if (change.newQuantity() == 0 && change.previousQuantity() != 0) {
            bus.publish(new Events.OutOfStock(change.productId(), change.productName(), change.timestamp()));
        } else if (change.newQuantity() > 0 && change.newQuantity() <= change.threshold()
                && change.previousQuantity() > change.threshold()) {
            bus.publish(new Events.LowStock(change.productId(), change.productName(),
                    change.newQuantity(), change.timestamp()));
        }
    }

    public synchronized List<Product> products() {
        return new ArrayList<>(products);
    }

    public synchronized List<StockMovement> movements() {
        List<StockMovement> result = new ArrayList<>(movements);
        result.sort(Comparator.comparing(StockMovement::createdAt).reversed());
        return result;
    }

    public synchronized Product add(String name, String category, long price, int quantity,
                                    int threshold, String imageUrl) throws IOException {
        validate(name, category, price, threshold);
        if (quantity < 0) {
            throw new IllegalArgumentException("Initial quantity cannot be negative.");
        }
        int id = 1;
        for (Product product : products) {
            id = Math.max(id, product.id() + 1);
        }
        Product product = new Product(id, name.trim(), category.trim(), price, quantity,
                threshold, cleanUrl(imageUrl));
        List<Product> next = new ArrayList<>(products);
        next.add(product);
        storage.save(new StorageService.Data(next, movements));
        products = next;
        bus.publish(new Events.ProductChanged(id, product.name(), "added", LocalDateTime.now()));
        return product;
    }

    public synchronized Product update(int id, String name, String category, long price,
                                       int threshold, String imageUrl) throws IOException {
        validate(name, category, price, threshold);
        int index = indexOf(id);
        Product old = products.get(index);
        Product updated = new Product(id, name.trim(), category.trim(), price, old.quantity(),
                threshold, cleanUrl(imageUrl));
        List<Product> next = new ArrayList<>(products);
        next.set(index, updated);
        storage.save(new StorageService.Data(next, movements));
        products = next;
        bus.publish(new Events.ProductChanged(id, updated.name(), "updated", LocalDateTime.now()));
        return updated;
    }

    public synchronized void delete(int id) throws IOException {
        int index = indexOf(id);
        Product old = products.get(index);
        List<Product> next = new ArrayList<>(products);
        next.remove(index);
        storage.save(new StorageService.Data(next, movements));
        products = next;
        bus.publish(new Events.ProductChanged(id, old.name(), "deleted", LocalDateTime.now()));
    }

    public synchronized StockMovement move(int id, String type, int quantity, String note) throws IOException {
        int index = indexOf(id);
        Product old = products.get(index);
        if (!"IN".equals(type) && !"OUT".equals(type)) {
            throw new IllegalArgumentException("Choose Stock In or Stock Out.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be a positive integer.");
        }
        if ("OUT".equals(type) && quantity > old.quantity()) {
            throw new IllegalArgumentException("Stock Out exceeds the available quantity.");
        }
        long result = "IN".equals(type) ? (long) old.quantity() + quantity : old.quantity() - quantity;
        if (result > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Stock quantity is too large.");
        }
        int newQuantity = (int) result;
        int movementId = 1;
        for (StockMovement existing : movements) {
            movementId = Math.max(movementId, existing.id() + 1);
        }
        LocalDateTime time = LocalDateTime.now();
        StockMovement movement = new StockMovement(movementId, id, old.name(), type, quantity,
                old.quantity(), newQuantity, note == null ? "" : note.trim(), time);
        List<Product> nextProducts = new ArrayList<>(products);
        nextProducts.set(index, old.withQuantity(newQuantity));
        List<StockMovement> nextMovements = new ArrayList<>(movements);
        nextMovements.add(movement);
        // Save first. A failed disk write cannot produce a success event.
        storage.save(new StorageService.Data(nextProducts, nextMovements));
        products = nextProducts;
        movements = nextMovements;
        bus.publish(new Events.StockChanged(id, old.name(), old.quantity(), newQuantity,
                old.lowStockThreshold(), type, time));
        return movement;
    }

    private int indexOf(int id) {
        for (int i = 0; i < products.size(); i++) {
            if (products.get(i).id() == id) {
                return i;
            }
        }
        throw new IllegalArgumentException("Product no longer exists.");
    }

    private static String cleanUrl(String url) {
        return url == null ? "" : url.trim();
    }

    private static void validate(String name, String category, long price, int threshold) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name is required.");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Category is required.");
        }
        if (price < 0) {
            throw new IllegalArgumentException("Price cannot be negative.");
        }
        if (threshold < 0) {
            throw new IllegalArgumentException("Low-stock threshold cannot be negative.");
        }
    }
}
