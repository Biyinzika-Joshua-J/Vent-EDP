package com.inventorymonitor.ui;

import com.inventorymonitor.event.EventListener;
import com.inventorymonitor.event.Events;
import com.inventorymonitor.ui.dialogs.ProductDialog;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;

public final class MainLayout extends BorderPane {
    private final AppContext context;
    private final StackPane content = new StackPane();
    private final List<Button> links = new ArrayList<>();
    private String current;
    private EventListener active;

    public MainLayout(AppContext context) {
        this.context = context;
        getStyleClass().add("app-root");
        context.bus.addListener(event -> {
            if (event instanceof Events.LowStock warning) {
                Platform.runLater(() -> toast("Low stock: " + warning.productName()
                        + " (" + warning.quantity() + " left)", false));
            } else if (event instanceof Events.OutOfStock warning) {
                Platform.runLater(() -> toast("Out of stock: " + warning.productName(), true));
            }
        });
        VBox sidebar = new VBox(7);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(200);
        sidebar.setMinWidth(200);
        HBox brand = new HBox(9, Ui.icon("bi-box-seam", 26), Ui.label("Vent", "brand-title"));
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.getStyleClass().add("brand");
        sidebar.getChildren().add(brand);
        for (String screen : List.of("Dashboard", "Products", "Stock Movements")) {
            String icon;
            if (screen.equals("Dashboard")) {
                icon = "bi-grid";
            } else if (screen.equals("Products")) {
                icon = "bi-box";
            } else {
                icon = "bi-arrow-left-right";
            }
            Button link = Ui.iconButton(screen, "nav-link", icon, 19);
            link.setMaxWidth(Double.MAX_VALUE);
            link.setAlignment(Pos.CENTER_LEFT);
            link.setOnAction(e -> navigate(screen, false));
            links.add(link);
            sidebar.getChildren().add(link);
        }
        setLeft(sidebar);
        setCenter(content);
        navigate("Dashboard", false);
    }

    public void navigate(String screen, boolean alertFilter) {
        if (screen.equals(current) && !alertFilter) {
            return;
        }
        showScreen(screen, alertFilter);
    }

    private void showScreen(String screen, boolean alertFilter) {
        if (active != null) {
            context.bus.removeListener(active);
        }
        current = screen;
        for (Button button : links) {
            button.getStyleClass().remove("nav-active");
            if (button.getText().equals(screen)) {
                button.getStyleClass().add("nav-active");
            }
        }

        String subtitle;
        if (screen.equals("Dashboard")) {
            subtitle = "Monitor stock levels and inventory activity.";
        } else if (screen.equals("Products")) {
            subtitle = "Manage your products and stock details.";
        } else {
            subtitle = "Record stock in or out and view inventory activity.";
        }
        VBox titles = new VBox(4, Ui.label(screen, "page-title"), Ui.label(subtitle, "subtitle"));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button add = Ui.iconButton("Add Product", "primary-button", "bi-plus", 19);
        add.setOnAction(e -> addProduct());
        HBox header = new HBox(titles, spacer, add);
        header.setAlignment(Pos.CENTER_LEFT);

        Node view;
        if (screen.equals("Dashboard")) {
            view = new DashboardView(context, () -> navigate("Products", true));
        } else if (screen.equals("Products")) {
            view = new ProductsView(context, alertFilter);
        } else {
            view = new StockMovementsView(context);
        }
        active = (EventListener) view;
        VBox page = new VBox(22, header, view);
        page.getStyleClass().add("page");
        VBox.setVgrow(view, Priority.ALWAYS);
        content.getChildren().setAll(page);
    }

    private void addProduct() {
        ProductDialog.show(null).ifPresent(values -> {
            context.submit(
                    () -> context.inventory.add(
                            values.name(),
                            values.category(),
                            values.price(),
                            values.quantity(),
                            values.threshold(),
                            values.imageUrl()),
                    () -> {});
        });
    }

    private void toast(String message, boolean danger) {
        Label notice = Ui.label(message, "toast");
        notice.getStyleClass().add(danger ? "toast-danger" : "toast-warning");
        StackPane.setAlignment(notice, Pos.BOTTOM_CENTER);
        StackPane.setMargin(notice, new Insets(0, 0, 22, 0));
        content.getChildren().add(notice);
        context.scheduler.afterSeconds(() -> Platform.runLater(() -> content.getChildren().remove(notice)), 4);
    }
}
