package com.inventorymonitor;

import com.inventorymonitor.service.StorageService;
import com.inventorymonitor.ui.*;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.nio.file.Path;
import java.io.IOException;

public final class Main extends Application {
    private AppContext context;
    private Exception startupError;

    @Override
    public void init() {
        try {
            context = new AppContext(new StorageService(Path.of("data", "inventory.json")));
        } catch (IOException error) {
            startupError = error;
        }
    }

    @Override
    public void start(Stage stage) {
        if (startupError != null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Unable to load inventory.json: " + startupError.getMessage());
            alert.setHeaderText("Inventory data could not be loaded");
            alert.showAndWait();
            Platform.exit();
            return;
        }
        var usableScreen = Screen.getPrimary().getVisualBounds();
        double initialWidth = Math.min(1280, usableScreen.getWidth() * 0.92);
        double initialHeight = Math.min(800, usableScreen.getHeight() * 0.90);
        Scene scene = new Scene(new MainLayout(context), initialWidth, initialHeight);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.initStyle(StageStyle.DECORATED);
        stage.setTitle("Vent");
        stage.setScene(scene);
        stage.setWidth(initialWidth);
        stage.setHeight(initialHeight);
        stage.setMinWidth(Math.min(1050, initialWidth));
        stage.setMinHeight(Math.min(650, initialHeight));
        stage.setResizable(true);
        stage.setOnCloseRequest(e -> context.close());
        stage.show();
        stage.centerOnScreen();
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
