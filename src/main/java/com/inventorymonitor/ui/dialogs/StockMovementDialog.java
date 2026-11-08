package com.inventorymonitor.ui.dialogs;

import com.inventorymonitor.model.Product;
import com.inventorymonitor.ui.Ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.Optional;

/** Collects one Stock In or Stock Out command. */
public final class StockMovementDialog {
    public record Values(int productId, String type, int quantity, String note) {}

    public static Optional<Values> show(Product product, String type) {
        Dialog<Values> dialog = new Dialog<>();
        dialog.setTitle(type.equals("IN") ? "Stock In" : "Stock Out");
        ButtonType save = new ButtonType("Record Movement", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        Label name = new Label(product.name() + " (" + product.quantity() + " available)");
        TextField quantity = new TextField();
        quantity.setPromptText("Quantity");
        TextField note = new TextField();
        note.setPromptText("Optional note");
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(18));
        grid.add(new Label("Product"), 0, 0);
        grid.add(name, 1, 0);
        grid.add(new Label("Quantity"), 0, 1);
        grid.add(quantity, 1, 1);
        grid.add(new Label("Note"), 0, 2);
        grid.add(note, 1, 2);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getStylesheets().add(StockMovementDialog.class.getResource("/styles.css").toExternalForm());

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(save);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            int amount;
            try {
                amount = Integer.parseInt(quantity.getText().trim());
            } catch (NumberFormatException error) {
                event.consume();
                Ui.error("Quantity must be a positive whole number.");
                return;
            }
            if (amount <= 0) {
                event.consume();
                Ui.error("Quantity must be greater than zero.");
            } else if (type.equals("OUT") && amount > product.quantity()) {
                event.consume();
                Ui.error("Stock Out exceeds the available quantity.");
            }
        });
        dialog.setResultConverter(button -> button == save
                ? new Values(product.id(), type, Integer.parseInt(quantity.getText().trim()), note.getText())
                : null);
        return dialog.showAndWait();
    }
}
