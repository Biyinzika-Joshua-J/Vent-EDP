package com.inventorymonitor.ui;

import com.inventorymonitor.model.Product;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URI;
import java.time.format.DateTimeFormatter;

public final class Ui {
    private Ui() {}

    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    public static Label label(String text, String style) {
        Label l = new Label(text);
        if (style != null) {
            l.getStyleClass().add(style);
        }
        return l;
    }

    public static Button button(String text, String style) {
        Button b = new Button(text);
        b.getStyleClass().add(style);
        return b;
    }

    public static FontIcon icon(String literal, int size) {
        FontIcon icon = new FontIcon(literal);
        icon.setIconSize(size);
        return icon;
    }

    public static Button iconButton(String text, String style, String literal, int size) {
        Button button = button(text, style);
        button.setGraphic(icon(literal, size));
        return button;
    }

    public static VBox card(Node... children) {
        VBox box = new VBox(14, children);
        box.getStyleClass().add("card");
        return box;
    }

    public static Label badge(String status) {
        Label badge = label(status, "badge");
        String color;
        String symbol;
        if (status.equals("In Stock") || status.equals("IN")) {
            color = "badge-green";
            symbol = "bi-check-circle-fill";
        } else if (status.equals("Low Stock")) {
            color = "badge-orange";
            symbol = "bi-exclamation-triangle-fill";
        } else {
            color = "badge-red";
            symbol = "bi-x-circle-fill";
        }
        badge.getStyleClass().add(color);
        badge.setGraphic(icon(symbol, 11));
        return badge;
    }

    public static ImageView image(Product product, int size) {
        ImageView view = new ImageView();
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(false);
        Rectangle clip = new Rectangle(size, size);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        view.setClip(clip);
        view.setImage(placeholder());
        loadImage(view, product.imageUrl(), size);
        return view;
    }

    public static void loadImage(ImageView view, String url, int size) {
        view.setImage(placeholder());
        view.setUserData(url);
        if (url == null || url.isBlank()) {
            return;
        }
        try {
            String scheme = URI.create(url).getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                return;
            }
            Image photo = new Image(url, size * 2, size * 2, true, true, true);
            photo.progressProperty().addListener((observable, oldValue, newValue) ->
                    showPhotoWhenReady(view, url, photo));
            showPhotoWhenReady(view, url, photo);
        } catch (IllegalArgumentException ignored) {
            // An invalid optional URL keeps the placeholder.
        }
    }

    private static void showPhotoWhenReady(ImageView view, String url, Image photo) {
        if (photo.getProgress() < 1 || photo.isError()) {
            return;
        }
        // The dialog URL may have changed while this photo was loading.
        Platform.runLater(() -> {
            if (url.equals(view.getUserData())) {
                view.setImage(photo);
            }
        });
    }

    private static Image placeholder() {
        // A bundled vector-like raster placeholder, avoiding network and disk access.
        javafx.scene.image.WritableImage image = new javafx.scene.image.WritableImage(8, 8);
        javafx.scene.image.PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                writer.setColor(x, y, javafx.scene.paint.Color.web((x + y) % 2 == 0 ? "#E9F8EF" : "#DDF1E5"));
            }
        }
        return image;
    }

    public static void error(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText("Unable to complete action");
        alert.showAndWait();
    }

}
