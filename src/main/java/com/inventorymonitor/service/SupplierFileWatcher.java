package com.inventorymonitor.service;

import com.inventorymonitor.event.EventBus;
import com.inventorymonitor.event.Events;
import com.inventorymonitor.model.Product;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;

public final class SupplierFileWatcher implements AutoCloseable {
    private final Path incoming;
    private final Path processed;
    private final InventoryService inventory;
    private final EventBus bus;
    private final ExecutorService worker;
    private final WatchService watcher;
    private final Thread thread;

    public SupplierFileWatcher(Path incoming, InventoryService inventory, EventBus bus,
                               ExecutorService worker) throws IOException {
        this.incoming = incoming;
        this.processed = incoming.resolveSibling("processed");
        this.inventory = inventory;
        this.bus = bus;
        this.worker = worker;
        Files.createDirectories(incoming);
        Files.createDirectories(processed);
        watcher = FileSystems.getDefault().newWatchService();
        incoming.register(watcher, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY);
        // WatchService.take() blocks, so this thread must never be the JavaFX thread.
        thread = new Thread(this::waitForFiles, "supplier-file-watcher");
        thread.setDaemon(true);
        thread.start();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(incoming, "*.txt")) {
            for (Path file : files) {
                sendToWorker(file);
            }
        }
    }

    private void waitForFiles() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                WatchKey key = watcher.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path name = (Path) event.context();
                    if (name.toString().endsWith(".txt")) {
                        sendToWorker(incoming.resolve(name));
                    }
                }
                if (!key.reset()) {
                    return;
                }
            } catch (InterruptedException | ClosedWatchServiceException ignored) {
                return;
            }
        }
    }

    private void sendToWorker(Path file) {
        // The watcher only notices files. The inventory worker reads and saves them.
        worker.execute(() -> {
            try {
                process(file);
            } catch (IOException | IllegalArgumentException error) {
                System.err.println("Supplier file " + file.getFileName() + ": " + error.getMessage());
            }
        });
    }

    public void process(Path file) throws IOException {
        if (!Files.exists(file)) {
            return;
        }
        // Step 1: read and check "product ID,quantity".
        String[] parts = Files.readString(file).trim().split(",", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Expected product ID,quantity");
        }
        int id;
        int amount;
        try {
            id = Integer.parseInt(parts[0].trim());
            amount = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Product ID and quantity must be whole numbers");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        Product product = findProduct(id);
        // Step 2: use the same stock operation as the GUI.
        inventory.move(id, "IN", amount, "Supplier delivery: " + file.getFileName());
        // Step 3: archive the file and announce the successful delivery.
        Files.move(file, processed.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        bus.publish(new Events.SupplierDeliveryReceived(id, product.name(), amount, LocalDateTime.now()));
    }

    private Product findProduct(int id) {
        for (Product product : inventory.products()) {
            if (product.id() == id) {
                return product;
            }
        }
        throw new IllegalArgumentException("Product ID does not exist: " + id);
    }

    @Override
    public void close() {
        try {
            watcher.close();
        } catch (IOException error) {
            System.err.println("Could not close supplier watcher: " + error.getMessage());
        }
        thread.interrupt();
    }
}
