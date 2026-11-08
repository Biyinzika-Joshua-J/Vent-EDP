package com.inventorymonitor.ui;

import com.inventorymonitor.event.*;
import com.inventorymonitor.service.*;

import javafx.application.Platform;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.*;

public final class AppContext implements AutoCloseable {
    public final EventBus bus;
    public final EventLog log;
    public final InventoryService inventory;
    public final InventoryScheduler scheduler;
    public final SupplierFileWatcher supplierWatcher;
    // One worker keeps all inventory file writes sequential and off the JavaFX thread.
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> new Thread(r, "inventory-storage"));

    public AppContext(StorageService storage) throws IOException {
        bus = new EventBus();
        log = new EventLog(bus);
        inventory = new InventoryService(storage, bus);
        scheduler = new InventoryScheduler(bus);
        Path parent = storage.path().toAbsolutePath().getParent();
        supplierWatcher = new SupplierFileWatcher(parent.resolveSibling("incoming"), inventory, bus, worker);
    }

    public void submit(Callable<?> work, Runnable success) {
        submit(work, success, () -> {});
    }

    public void submit(Callable<?> work, Runnable success, Runnable finishedAfterError) {
        worker.submit(
                () -> {
                    try {
                        work.call();
                        Platform.runLater(success);
                    } catch (Exception error) {
                        Platform.runLater(
                                () -> {
                                    finishedAfterError.run();
                                    if (error instanceof IOException) {
                                        Ui.error("Could not save inventory data: " + error.getMessage());
                                    } else {
                                        Ui.error(error.getMessage());
                                    }
                                });
                    }
                });
    }

    @Override
    public void close() {
        supplierWatcher.close();
        scheduler.close();
        worker.shutdown();
    }
}
