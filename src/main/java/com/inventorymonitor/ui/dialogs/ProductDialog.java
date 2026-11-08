package com.inventorymonitor.ui.dialogs;

import com.inventorymonitor.model.Product;
import com.inventorymonitor.ui.PriceFormat;
import com.inventorymonitor.ui.Ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Optional;

public final class ProductDialog {
    public record Values(String name, String category, long price, int quantity, int threshold, String imageUrl) {}

    public static Optional<Values> show(Product existing) {
        Dialog<Values> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Product" : "Edit Product");
        ButtonType save = new ButtonType("Save Product", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        TextField name = field(existing == null ? "" : existing.name());
        TextField category = field(existing == null ? "" : existing.category());
        TextField quantity = field(existing == null ? "0" : Integer.toString(existing.quantity()));
        TextField price = field(existing == null ? "0" : PriceFormat.format(existing.price()));
        TextField threshold = field(existing == null ? "10" : Integer.toString(existing.lowStockThreshold()));
        TextField url = field(existing == null ? "" : existing.imageUrl());
        url.setPromptText("https://example.com/image.jpg");
        if (existing != null) {
            quantity.setDisable(true);
            quantity.setTooltip(new Tooltip("Use Stock Movements to change quantity."));
        }
        javafx.scene.image.ImageView preview =
                Ui.image(existing == null ? new Product(0, "", "", 0, 0, 0, "") : existing, 48);
        url.textProperty().addListener((o, old, value) -> Ui.loadImage(preview, value, 48));
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(18));
        String[] captions = {
            "Product Name", "Category", "Initial Quantity", "Unit Price (UGX)", "Low Stock Threshold", "Image URL"
        };
        TextField[] fields = {name, category, quantity, price, threshold, url};
        for (int i = 0; i < fields.length; i++) {
            grid.add(new Label(captions[i]), 0, i);
            grid.add(fields[i], 1, i);
        }
        grid.add(preview, 1, 6);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getStylesheets().add(ProductDialog.class.getResource("/styles.css").toExternalForm());
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(save);
        saveButton.addEventFilter(
                javafx.event.ActionEvent.ACTION,
                event -> {
                    try {
                        validate(
                                name.getText(),
                                category.getText(),
                                price.getText(),
                                quantity.getText(),
                                threshold.getText());
                    } catch (IllegalArgumentException error) {
                        event.consume();
                        Ui.error(error.getMessage());
                    }
                });
        dialog.setResultConverter(
                button ->
                        button == save
                                ? new Values(
                                        name.getText().trim(),
                                        category.getText().trim(),
                                        PriceFormat.parse(price.getText()),
                                        Integer.parseInt(quantity.getText().trim()),
                                        Integer.parseInt(threshold.getText().trim()),
                                        url.getText().trim())
                                : null);
        return dialog.showAndWait();
    }

    private static TextField field(String value) {
        TextField field = new TextField(value);
        field.setPrefWidth(300);
        return field;
    }

    private static void validate(
            String name, String category, String price, String quantity, String threshold) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Product name is required.");
        }
        if (category.isBlank()) {
            throw new IllegalArgumentException("Category is required.");
        }
        PriceFormat.parse(price);
        int initialQuantity;
        int lowThreshold;
        try {
            initialQuantity = Integer.parseInt(quantity.trim());
            lowThreshold = Integer.parseInt(threshold.trim());
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Enter whole, non-negative numbers for quantity and threshold.");
        }
        if (initialQuantity < 0 || lowThreshold < 0) {
            throw new IllegalArgumentException("Numeric values cannot be negative.");
        }
    }
}
