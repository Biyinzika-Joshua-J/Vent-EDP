package com.inventorymonitor.model;

public record Product(
        int id,
        String name,
        String category,
        long price,
        int quantity,
        int lowStockThreshold,
        String imageUrl)
{
    public Product withQuantity(int value) {
        return new Product(id, name, category, price, value, lowStockThreshold, imageUrl);
    }

    public String status() {
        return quantity == 0 ? "Out of Stock" : quantity <= lowStockThreshold ? "Low Stock" : "In Stock";
    }
}
