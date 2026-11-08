package com.inventorymonitor.event;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class EventLog implements EventListener {
    public record Entry(String type, LocalDateTime timestamp, String description) {}

    private final List<Entry> entries = new ArrayList<>();

    public EventLog(EventBus bus) {
        bus.addListener(this);
    }

    @Override
    public void onEvent(AppEvent event) {
        if (event instanceof Events.ProductChanged change) {
            add("Product " + change.action(), change.timestamp(), change.productName());
        } else if (event instanceof Events.StockChanged change) {
            String action = change.type().equals("IN") ? "received" : "sold";
            add("Stock " + action, change.timestamp(),
                    change.productName() + ": " + change.previousQuantity() + " to " + change.newQuantity());
        } else if (event instanceof Events.LowStock warning) {
            add("Low stock warning", warning.timestamp(),
                    warning.productName() + " has " + warning.quantity() + " left");
        } else if (event instanceof Events.OutOfStock warning) {
            add("Out of stock warning", warning.timestamp(),
                    warning.productName() + " is out of stock");
        } else if (event instanceof Events.InventoryReview review) {
            add("Inventory review", review.timestamp(), "Scheduled stock review completed");
        } else if (event instanceof Events.SupplierDeliveryReceived delivery) {
            add("Supplier delivery", delivery.timestamp(),
                    delivery.productName() + ": " + delivery.quantity() + " received");
        }
    }

    private synchronized void add(String type, LocalDateTime time, String description) {
        entries.add(0, new Entry(type, time, description));
        if (entries.size() > 100) {
            entries.remove(entries.size() - 1);
        }
    }

    public synchronized List<Entry> recent() {
        return new ArrayList<>(entries);
    }
}
