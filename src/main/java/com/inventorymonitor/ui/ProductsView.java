package com.inventorymonitor.ui;

import com.inventorymonitor.event.AppEvent;
import com.inventorymonitor.event.EventListener;
import com.inventorymonitor.event.Events;
import com.inventorymonitor.model.Product;
import com.inventorymonitor.ui.dialogs.ProductDialog;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;

public final class ProductsView extends VBox implements EventListener {
    private final AppContext context;
    private final TableView<Product> table = new TableView<>();
    private final ComboBox<String> category = new ComboBox<>(), status = new ComboBox<>();
    private boolean refreshing;

    public ProductsView(AppContext context, boolean alertsOnly) {
        this.context = context;
        setSpacing(16);
        category.setPromptText("Category");
        status.getItems().addAll("All Statuses", "In Stock", "Low Stock", "Out of Stock", "Low / Out of Stock");
        status.setValue(alertsOnly ? "Low / Out of Stock" : "All Statuses");
        category.setValue("All Categories");
        category.setOnAction(e -> filterChanged());
        status.setOnAction(e -> filterChanged());
        HBox filters = new HBox(10, category, status);
        filters.setAlignment(Pos.CENTER_LEFT);
        buildTable();
        VBox card = Ui.card(filters, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(card, Priority.ALWAYS);
        getChildren().add(card);
        context.bus.addListener(this);
        refresh();
    }

    private void buildTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(Ui.label("No products match the selected filters.", "muted"));
        TableColumn<Product, String> product = new TableColumn<>("Product");
        product.setPrefWidth(230);
        product.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().name()));
        product.setCellFactory(column -> productCell());
        TableColumn<Product, String> cat = new TableColumn<>("Category");
        cat.setPrefWidth(110);
        cat.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().category()));
        TableColumn<Product, String> qty = new TableColumn<>("Quantity");
        qty.setPrefWidth(90);
        qty.setCellValueFactory(row -> new SimpleStringProperty(Integer.toString(row.getValue().quantity())));
        TableColumn<Product, String> threshold = new TableColumn<>("Low Stock Threshold");
        threshold.setPrefWidth(150);
        threshold.setCellValueFactory(row ->
                new SimpleStringProperty(Integer.toString(row.getValue().lowStockThreshold())));
        TableColumn<Product, String> price = new TableColumn<>("Price (UGX)");
        price.setPrefWidth(120);
        price.setCellValueFactory(row -> new SimpleStringProperty(PriceFormat.format(row.getValue().price())));
        TableColumn<Product, String> state = new TableColumn<>("Status");
        state.setPrefWidth(120);
        state.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().status()));
        state.setCellFactory(column -> statusCell());
        TableColumn<Product, Void> actions = new TableColumn<>("Actions");
        actions.setPrefWidth(90);
        actions.setCellFactory(column -> actionCell());
        table.getColumns().addAll(product, cat, qty, threshold, price, state, actions);
    }

    private TableCell<Product, String> productCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Product product = getTableView().getItems().get(getIndex());
                    setGraphic(new HBox(10, Ui.image(product, 34), Ui.label(value, "item-title")));
                }
                setText(null);
            }
        };
    }

    private TableCell<Product, String> statusCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setGraphic(empty ? null : Ui.badge(value));
                setText(null);
            }
        };
    }

    private TableCell<Product, Void> actionCell() {
        return new TableCell<>() {
            private final Button button = Ui.iconButton("", "text-button", "bi-three-dots", 18);

            @Override
            protected void updateItem(Void value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    button.setOnAction(event -> {
                        Product selected = getTableView().getItems().get(getIndex());
                        showActions(selected, button);
                    });
                    setGraphic(button);
                }
            }
        };
    }

    private void showActions(Product selected, Button button) {
        MenuItem editItem = new MenuItem("Edit");
        MenuItem deleteItem = new MenuItem("Delete");
        editItem.setOnAction(event -> edit(selected));
        deleteItem.setOnAction(event -> delete(selected));
        new ContextMenu(editItem, deleteItem).show(button, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private void edit(Product product) {
        ProductDialog.show(product).ifPresent(values -> {
            context.submit(
                    () -> context.inventory.update(
                            product.id(),
                            values.name(),
                            values.category(),
                            values.price(),
                            values.threshold(),
                            values.imageUrl()),
                    () -> {});
        });
    }

    private void delete(Product product) {
        Alert confirmation =
                new Alert(
                        Alert.AlertType.CONFIRMATION,
                        "Delete " + product.name() + "? Existing movement history will remain.",
                        ButtonType.CANCEL,
                        ButtonType.OK);
        confirmation.setHeaderText("Delete product");
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            context.submit(
                    () -> {
                        context.inventory.delete(product.id());
                        return null;
                    },
                    () -> {});
        }
    }

    private void filterChanged() {
        if (!refreshing) {
            refresh();
        }
    }

    public void refresh() {
        if (refreshing) {
            return;
        }
        refreshing = true;
        List<Product> all = context.inventory.products();
        String chosenCategory = updateCategories(all);
        List<Product> filtered = matchingProducts(all, chosenCategory);
        table.setItems(FXCollections.observableArrayList(filtered));
        refreshing = false;
    }

    private String updateCategories(List<Product> products) {
        String chosen = category.getValue();
        List<String> categories = new ArrayList<>();
        categories.add("All Categories");
        for (Product product : products) {
            if (!categories.contains(product.category())) {
                categories.add(product.category());
            }
        }
        categories.subList(1, categories.size()).sort(String::compareTo);
        category.getItems().setAll(categories);
        if (!categories.contains(chosen)) {
            chosen = "All Categories";
        }
        category.setValue(chosen);
        return chosen;
    }

    private List<Product> matchingProducts(List<Product> products, String chosenCategory) {
        List<Product> matches = new ArrayList<>();
        String chosenStatus = status.getValue();
        for (Product product : products) {
            boolean categoryMatches = chosenCategory.equals("All Categories")
                    || product.category().equals(chosenCategory);
            boolean statusMatches = chosenStatus == null || chosenStatus.equals("All Statuses")
                    || (chosenStatus.equals("Low / Out of Stock") && !product.status().equals("In Stock"))
                    || product.status().equals(chosenStatus);
            if (categoryMatches && statusMatches) {
                matches.add(product);
            }
        }
        return matches;
    }

    @Override
    public void onEvent(AppEvent event) {
        if (event instanceof Events.ProductChanged || event instanceof Events.StockChanged) {
            Platform.runLater(this::refresh);
        }
    }
}
