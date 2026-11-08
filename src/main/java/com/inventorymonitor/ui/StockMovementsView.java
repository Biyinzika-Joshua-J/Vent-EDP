package com.inventorymonitor.ui;

import com.inventorymonitor.event.AppEvent;
import com.inventorymonitor.event.EventListener;
import com.inventorymonitor.event.Events;
import com.inventorymonitor.model.Product;
import com.inventorymonitor.model.StockMovement;
import com.inventorymonitor.ui.dialogs.StockMovementDialog;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;

import java.util.List;

public final class StockMovementsView extends VBox implements EventListener {
    private final AppContext context;
    private final ComboBox<Product> product = new ComboBox<>();
    private final ComboBox<String> type = new ComboBox<>();
    private final TextField quantity = new TextField(), note = new TextField();
    private final Label feedback = Ui.label("", "success-text");
    private final TableView<StockMovement> table = new TableView<>();
    private final Button recordButton = Ui.button("Record Movement", "primary-button");

    public StockMovementsView(AppContext context) {
        this.context = context;
        setSpacing(20);
        product.setPromptText("Select product");
        product.setConverter(
                new javafx.util.StringConverter<>() {
                    public String toString(Product p) {
                        return p == null ? "" : p.name() + " (" + p.quantity() + " available)";
                    }

                    public Product fromString(String s) {
                        return null;
                    }
                });
        product.setPrefWidth(250);
        type.getItems().addAll("Stock In", "Stock Out");
        type.setValue("Stock In");
        type.setPrefWidth(130);
        quantity.setPromptText("Quantity");
        quantity.setPrefWidth(100);
        note.setPromptText("Optional note");
        note.setPrefWidth(200);
        recordButton.setTooltip(new Tooltip("Quick entry. Ctrl+I or Ctrl+O opens a stock dialog."));
        recordButton.setOnAction(e -> record());
        quantity.setOnAction(e -> record());
        FlowPane form = new FlowPane(10, 10, product, type, quantity, note, recordButton);
        form.setAlignment(Pos.CENTER_LEFT);
        VBox formCard = Ui.card(Ui.label("Record Stock Movement", "section-title"), form, feedback);
        buildTable();
        VBox history = Ui.card(Ui.label("Movement History", "section-title"), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(history, Priority.ALWAYS);
        getChildren().addAll(formCard, history);
        addEventFilter(KeyEvent.KEY_PRESSED, this::handleShortcut);
        context.bus.addListener(this);
        refresh();
    }

    private void record() {
        if (recordButton.isDisabled()) {
            return;
        }
        Product p = product.getValue();
        if (p == null) {
            Ui.error("Select a product.");
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(quantity.getText().trim());
        } catch (NumberFormatException error) {
            Ui.error("Quantity must be a positive whole number.");
            return;
        }
        if (amount <= 0) {
            Ui.error("Quantity must be a positive whole number.");
            return;
        }
        String movementType = type.getValue().equals("Stock In") ? "IN" : "OUT";
        saveMovement(p.id(), movementType, amount, note.getText());
    }

    private void saveMovement(int productId, String movementType, int amount, String movementNote) {
        // The inline form and keyboard dialog use the same save path.
        recordButton.setDisable(true);
        feedback.setText("");
        context.submit(
                () -> context.inventory.move(productId, movementType, amount, movementNote),
                () -> {
                    recordButton.setDisable(false);
                    quantity.clear();
                    note.clear();
                    feedback.setText("Movement recorded successfully.");
                    refresh();
                },
                () -> recordButton.setDisable(false));
    }

    private void openMovementDialog(String movementType) {
        if (recordButton.isDisabled()) {
            return;
        }
        Product selected = product.getValue();
        if (selected == null) {
            Ui.error("Select a product.");
            return;
        }
        StockMovementDialog.show(selected, movementType).ifPresent(values ->
                saveMovement(values.productId(), values.type(), values.quantity(), values.note()));
    }

    private void handleShortcut(KeyEvent event) {
        if (!event.isControlDown()) {
            return;
        }
        if (event.getCode() == KeyCode.I) {
            openMovementDialog("IN");
        } else if (event.getCode() == KeyCode.O) {
            openMovementDialog("OUT");
        } else {
            return;
        }
        event.consume();
    }

    private void buildTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(Ui.label("No movements recorded yet.", "muted"));
        TableColumn<StockMovement, String> productName = new TableColumn<>("Product");
        productName.setPrefWidth(180);
        productName.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().productName()));

        TableColumn<StockMovement, String> movementType = new TableColumn<>("Movement Type");
        movementType.setPrefWidth(130);
        movementType.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().type().equals("IN") ? "Stock In" : "Stock Out"));
        movementType.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(Ui.badge(value.equals("Stock In") ? "IN" : "OUT"));
                }
                setText(null);
            }
        });

        TableColumn<StockMovement, String> amount = new TableColumn<>("Quantity");
        amount.setPrefWidth(90);
        amount.setCellValueFactory(row -> new SimpleStringProperty(Integer.toString(row.getValue().quantity())));

        TableColumn<StockMovement, String> previous = new TableColumn<>("Previous Stock");
        previous.setPrefWidth(120);
        previous.setCellValueFactory(row -> new SimpleStringProperty(
                Integer.toString(row.getValue().previousStock())));

        TableColumn<StockMovement, String> current = new TableColumn<>("New Stock");
        current.setPrefWidth(100);
        current.setCellValueFactory(row -> new SimpleStringProperty(Integer.toString(row.getValue().newStock())));

        TableColumn<StockMovement, String> date = new TableColumn<>("Date and Time");
        date.setPrefWidth(180);
        date.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().createdAt().format(Ui.TIME)));

        table.getColumns().addAll(productName, movementType, amount, previous, current, date);
    }

    public void refresh() {
        Integer selected = product.getValue() == null ? null : product.getValue().id();
        List<Product> all = context.inventory.products();
        product.setItems(FXCollections.observableArrayList(all));
        if (selected != null) {
            for (Product item : all) {
                if (item.id() == selected) {
                    product.setValue(item);
                    break;
                }
            }
        }
        table.setItems(FXCollections.observableArrayList(context.inventory.movements()));
    }

    @Override
    public void onEvent(AppEvent event) {
        if (event instanceof Events.ProductChanged || event instanceof Events.StockChanged) {
            Platform.runLater(this::refresh);
        }
    }
}
