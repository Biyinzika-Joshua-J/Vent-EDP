package com.inventorymonitor.event;

import java.time.LocalDateTime;

public final class Events {
    private Events() {}

    public record ProductChanged(int productId, String productName, String action,
                                 LocalDateTime timestamp) implements AppEvent {}

    public record StockChanged(int productId, String productName, int previousQuantity,
                               int newQuantity, int threshold, String type,
                               LocalDateTime timestamp) implements AppEvent {}

    public record LowStock(int productId, String productName, int quantity,
                           LocalDateTime timestamp) implements AppEvent {}

    public record OutOfStock(int productId, String productName,
                             LocalDateTime timestamp) implements AppEvent {}

    public record InventoryReview(LocalDateTime timestamp) implements AppEvent {}

    public record SupplierDeliveryReceived(int productId, String productName, int quantity,
                                           LocalDateTime timestamp) implements AppEvent {}
}
