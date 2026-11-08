package com.inventorymonitor;

import com.inventorymonitor.event.*;
import com.inventorymonitor.model.Product;
import com.inventorymonitor.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InventoryServiceTest {
    @TempDir Path directory;
    private Path file;
    private EventBus bus;
    private InventoryService service;
    private final List<AppEvent> received = new ArrayList<>();

    @BeforeEach
    void setup() throws Exception {
        file = directory.resolve("data/inventory.json");
        new StorageService(file).save(new StorageService.Data(List.of(), List.of()));
        bus = new EventBus();
        service = new InventoryService(new StorageService(file), bus);
        bus.addListener(received::add);
    }

    private Product add() throws IOException {
        return service.add("Milk", "Dairy", 4000, 12, 10, "");
    }

    private long count(Class<?> type) {
        long result = 0;
        for (AppEvent event : received) {
            if (type.isInstance(event)) result++;
        }
        return result;
    }

    @Test void createsAndEditsProduct() throws Exception {
        Product product = add();
        service.update(product.id(), "Fresh Milk", "Dairy", 5000, 8, "");
        assertEquals("Fresh Milk", service.products().get(0).name());
        assertEquals(12, service.products().get(0).quantity());
        assertEquals(2, count(Events.ProductChanged.class));
    }

    @Test void deletesProductButKeepsMovementHistory() throws Exception {
        Product product = add();
        service.move(product.id(), "OUT", 1, "Sale");
        service.delete(product.id());
        assertTrue(service.products().isEmpty());
        assertEquals("Milk", service.movements().get(0).productName());
    }

    @Test void recordsStockInAndOut() throws Exception {
        Product product = add();
        service.move(product.id(), "IN", 5, "Delivery");
        service.move(product.id(), "OUT", 3, "Sale");
        assertEquals(14, service.products().get(0).quantity());
        assertEquals(2, service.movements().size());
        assertEquals(2, count(Events.StockChanged.class));
    }

    @Test void rejectsInvalidProductAndMovementNumbers() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> service.add(" ", "Dairy", 1, 1, 1, ""));
        assertThrows(IllegalArgumentException.class, () -> service.add("Milk", "Dairy", -1, 1, 1, ""));
        Product product = add();
        assertThrows(IllegalArgumentException.class, () -> service.move(product.id(), "IN", 0, ""));
        assertThrows(IllegalArgumentException.class, () -> service.move(product.id(), "OUT", 13, ""));
        assertEquals(12, service.products().get(0).quantity());
        assertTrue(service.movements().isEmpty());
    }

    @Test void stockChangeChainsToOneLowStockWarning() throws Exception {
        Product product = add();
        service.move(product.id(), "OUT", 4, "");
        service.move(product.id(), "OUT", 1, "");
        assertEquals(1, count(Events.LowStock.class));
    }

    @Test void zeroStockProducesOutOfStockWarning() throws Exception {
        Product product = add();
        service.move(product.id(), "OUT", 12, "");
        assertEquals(1, count(Events.OutOfStock.class));
        assertEquals(0, count(Events.LowStock.class));
    }

    @Test void listenerCanBeRemoved() throws Exception {
        List<AppEvent> temporary = new ArrayList<>();
        EventListener listener = temporary::add;
        bus.addListener(listener);
        bus.removeListener(listener);
        add();
        assertTrue(temporary.isEmpty());
    }

    @Test void rapidSequentialEventsKeepCorrectBalance() throws Exception {
        Product product = add();
        for (int i = 0; i < 30; i++) service.move(product.id(), "IN", 1, "");
        assertEquals(42, service.products().get(0).quantity());
        assertEquals(30, service.movements().size());
    }

    @Test void unexpectedReviewOrderingDoesNotInventWarnings() throws Exception {
        bus.publish(new Events.InventoryReview(LocalDateTime.now()));
        Product product = add();
        bus.publish(new Events.InventoryReview(LocalDateTime.now()));
        service.move(product.id(), "OUT", 2, "");
        assertEquals(1, count(Events.LowStock.class));
    }

    @Test void failingListenerDoesNotBlockOthers() {
        bus.addListener(event -> { throw new IllegalStateException("test failure"); });
        bus.publish(new Events.InventoryReview(LocalDateTime.now()));
        assertEquals(1, count(Events.InventoryReview.class));
    }

    @Test void jsonDataSurvivesRestart() throws Exception {
        Product product = add();
        service.move(product.id(), "OUT", 2, "Sale");
        InventoryService loaded = new InventoryService(new StorageService(file), new EventBus());
        assertEquals(10, loaded.products().get(0).quantity());
        assertEquals(1, loaded.movements().size());
    }

    @Test void failedSaveDoesNotChangeStockOrPublishEvent() throws Exception {
        Product product = add();
        StorageService failingStorage = new StorageService(file) {
            @Override public void save(Data data) throws IOException { throw new IOException("Disk unavailable"); }
        };
        InventoryService isolated = new InventoryService(failingStorage, bus);
        int before = received.size();
        assertThrows(IOException.class, () -> isolated.move(product.id(), "OUT", 4, ""));
        assertEquals(12, isolated.products().get(0).quantity());
        assertEquals(before, received.size());
    }

    @Test void supplierFileUpdatesStockAndMovesToProcessed() throws Exception {
        Product product = add();
        Path incoming = directory.resolve("incoming");
        Files.createDirectories(incoming);
        Path delivery = incoming.resolve("delivery.txt");
        Files.writeString(delivery, product.id() + ",5");
        java.util.concurrent.ExecutorService worker = java.util.concurrent.Executors.newSingleThreadExecutor();
        try (SupplierFileWatcher watcher = new SupplierFileWatcher(incoming, service, bus, worker)) {
            worker.submit(() -> {}).get();
            assertEquals(17, service.products().get(0).quantity());
            assertTrue(Files.exists(directory.resolve("processed/delivery.txt")));
            assertEquals(1, count(Events.SupplierDeliveryReceived.class));
        } finally {
            worker.shutdownNow();
        }
    }

    @Test void malformedSupplierFileIsRejected() throws Exception {
        add();
        Path incoming = directory.resolve("incoming");
        Files.createDirectories(incoming);
        Path delivery = incoming.resolve("bad.txt");
        Files.writeString(delivery, "999,0");
        java.util.concurrent.ExecutorService worker = java.util.concurrent.Executors.newSingleThreadExecutor();
        try (SupplierFileWatcher watcher = new SupplierFileWatcher(incoming, service, bus, worker)) {
            worker.submit(() -> {}).get();
            assertTrue(Files.exists(delivery));
            assertEquals(12, service.products().get(0).quantity());
        } finally {
            worker.shutdownNow();
        }
    }

    @Test void watcherDetectsNewTextFile() throws Exception {
        Product product = add();
        Path incoming = directory.resolve("incoming");
        java.util.concurrent.ExecutorService worker = java.util.concurrent.Executors.newSingleThreadExecutor();
        java.util.concurrent.CountDownLatch delivered = new java.util.concurrent.CountDownLatch(1);
        bus.addListener(event -> {
            if (event instanceof Events.SupplierDeliveryReceived) {
                delivered.countDown();
            }
        });
        try (SupplierFileWatcher watcher = new SupplierFileWatcher(incoming, service, bus, worker)) {
            Files.writeString(incoming.resolve("new-delivery.txt"), product.id() + ",2");
            assertTrue(delivered.await(3, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(14, service.products().get(0).quantity());
            assertTrue(Files.exists(directory.resolve("processed/new-delivery.txt")));
        } finally {
            worker.shutdownNow();
        }
    }

    @Test void scheduledReviewPublishesEvent() throws Exception {
        try (InventoryScheduler scheduler = new InventoryScheduler(bus)) {
            scheduler.afterSeconds(() -> bus.publish(new Events.InventoryReview(LocalDateTime.now())), 0);
            for (int i = 0; i < 100 && count(Events.InventoryReview.class) == 0; i++) {
                Thread.sleep(10);
            }
            assertEquals(1, count(Events.InventoryReview.class));
        }
    }
}
