package com.inventorymonitor.ui;

import com.inventorymonitor.event.AppEvent;
import com.inventorymonitor.event.EventListener;
import com.inventorymonitor.event.Events;
import com.inventorymonitor.model.Product;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

public final class DashboardView extends VBox implements EventListener {
    private final AppContext context;
    private final VBox lowList = new VBox(10);
    private final VBox events = new VBox(12);
    private final Label totalProducts = Ui.label("0", "metric-number");
    private final Label totalStock = metricValue("metric-stock");
    private final Label lowStock = metricValue("metric-low-stock");
    private final Label outOfStock = Ui.label("0", "metric-number");

    public DashboardView(AppContext context, Runnable viewAll) {
        this.context = context;
        setSpacing(22);
        HBox metrics =
                new HBox(
                        16,
                        metric("Total Products", totalProducts),
                        metric("Total Stock", totalStock),
                        metric("Low Stock", lowStock),
                        metric("Out of Stock", outOfStock));
        for (javafx.scene.Node node : metrics.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
        }
        HBox lowHeader = new HBox();
        lowHeader.setAlignment(Pos.CENTER_LEFT);
        Label lowTitle = Ui.label("Low Stock Products", "section-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button view = Ui.iconButton("View All", "text-button", "bi-arrow-right", 15);
        view.setContentDisplay(ContentDisplay.RIGHT);
        view.setOnAction(e -> viewAll.run());
        lowHeader.getChildren().addAll(lowTitle, spacer, view);
        VBox lowCard = Ui.card(lowHeader, lowList);
        VBox eventCard = Ui.card(Ui.label("Recent Events", "section-title"), events);
        HBox panels = new HBox(16, lowCard, eventCard);
        HBox.setHgrow(lowCard, Priority.ALWAYS);
        HBox.setHgrow(eventCard, Priority.ALWAYS);
        lowCard.setMinWidth(0);
        eventCard.setMinWidth(0);
        lowCard.prefWidthProperty().bind(panels.widthProperty().multiply(0.52));
        eventCard.prefWidthProperty().bind(panels.widthProperty().multiply(0.48));
        getChildren().addAll(metrics, panels);
        context.bus.addListener(this);
        refresh();
    }

    private VBox metric(String name, Label value) {
        VBox card = Ui.card(Ui.label(name, "muted"), value);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private static Label metricValue(String colorClass) {
        Label value = Ui.label("0", "metric-number");
        value.getStyleClass().add(colorClass);
        return value;
    }

    public void refresh() {
        List<Product> products = context.inventory.products();
        totalProducts.setText(Integer.toString(products.size()));
        long stockCount = 0;
        int lowCount = 0;
        int outCount = 0;
        List<Product> alerts = new ArrayList<>();
        for (Product product : products) {
            stockCount += product.quantity();
            if (product.status().equals("Low Stock")) {
                lowCount++;
                alerts.add(product);
            } else if (product.status().equals("Out of Stock")) {
                outCount++;
                alerts.add(product);
            }
        }
        totalStock.setText(Long.toString(stockCount));
        lowStock.setText(Integer.toString(lowCount));
        outOfStock.setText(Integer.toString(outCount));
        lowList.getChildren().clear();
        alerts.sort(Comparator.comparingInt(Product::quantity));
        if (alerts.isEmpty()) {
            lowList.getChildren().add(Ui.label("All products have healthy stock levels.", "muted"));
        }
        for (int i = 0; i < Math.min(5, alerts.size()); i++) {
            Product p = alerts.get(i);
            VBox details =
                    new VBox(
                            3,
                            Ui.label(p.name(), "item-title"),
                            Ui.label(p.quantity() + " in stock  ·  threshold " + p.lowStockThreshold(), "muted"));
            Region space = new Region();
            HBox.setHgrow(space, Priority.ALWAYS);
            HBox row = new HBox(10, Ui.image(p, 38), details, space, Ui.badge(p.status()));
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("list-row");
            lowList.getChildren().add(row);
        }
        events.getChildren().clear();
        var recent = context.log.recent();
        if (recent.isEmpty()) {
            events.getChildren().add(Ui.label("No inventory events yet.", "muted"));
        }
        for (int i = 0; i < Math.min(5, recent.size()); i++) {
            var event = recent.get(i);
            var icon = Ui.icon("bi-clock-history", 15);
            icon.getStyleClass().add("event-icon");
            VBox details =
                    new VBox(
                            3,
                            Ui.label(event.type() + " · " + event.description(), "item-title"),
                            Ui.label(event.timestamp().format(Ui.TIME), "muted"));
            HBox row = new HBox(9, icon, details);
            row.setAlignment(Pos.TOP_LEFT);
            row.getStyleClass().add("list-row");
            events.getChildren().add(row);
        }
    }

    @Override
    public void onEvent(AppEvent event) {
        if (event instanceof Events.ProductChanged || event instanceof Events.StockChanged
                || event instanceof Events.LowStock || event instanceof Events.OutOfStock
                || event instanceof Events.InventoryReview || event instanceof Events.SupplierDeliveryReceived) {
            Platform.runLater(this::refresh);
        }
    }
}
