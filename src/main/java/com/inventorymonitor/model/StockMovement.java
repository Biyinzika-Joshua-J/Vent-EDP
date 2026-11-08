package com.inventorymonitor.model;

import java.time.LocalDateTime;

public record StockMovement(
        int id,
        int productId,
        String productName,
        String type,
        int quantity,
        int previousStock,
        int newStock,
        String note,
        LocalDateTime createdAt) {}
