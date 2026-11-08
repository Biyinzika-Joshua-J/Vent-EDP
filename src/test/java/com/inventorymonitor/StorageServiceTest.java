package com.inventorymonitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventorymonitor.event.AppEvent;
import com.inventorymonitor.event.EventBus;
import com.inventorymonitor.model.Product;
import com.inventorymonitor.service.InventoryService;
import com.inventorymonitor.service.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StorageServiceTest {
    @TempDir Path directory;

    private StorageService storage() {
        return new StorageService(directory.resolve("data/inventory.json"));
    }

    @Test
    void missingFileCreatesEmptyInventory() throws Exception {
        StorageService storage = storage();
        assertFalse(Files.exists(storage.path()));

        StorageService.Data data = storage.load();

        assertTrue(data.products().isEmpty());
        assertTrue(data.movements().isEmpty());
        assertTrue(Files.exists(storage.path()));
        JsonNode json = new ObjectMapper().readTree(storage.path().toFile());
        assertTrue(json.get("products").isEmpty());
        assertTrue(json.get("movements").isEmpty());
    }

    @Test
    void existingEmptyFileStaysEmpty() throws Exception {
        StorageService storage = storage();
        storage.load();
        String saved = Files.readString(storage.path());

        assertTrue(new StorageService(storage.path()).load().products().isEmpty());
        assertEquals(saved, Files.readString(storage.path()));
    }

    @Test
    void addedProductAndMovementSurviveRestart() throws Exception {
        StorageService storage = storage();
        InventoryService inventory = new InventoryService(storage, new EventBus());
        Product product = inventory.add("Notebook", "Stationery", 3000, 8, 2, "");
        assertEquals(1, product.id());
        inventory.move(product.id(), "OUT", 3, "Sale");

        InventoryService restarted = new InventoryService(storage, new EventBus());
        assertEquals(1, restarted.products().size());
        assertEquals("Notebook", restarted.products().get(0).name());
        assertEquals(5, restarted.products().get(0).quantity());
        assertEquals(1, restarted.movements().size());
        assertEquals("Sale", restarted.movements().get(0).note());
    }

    @Test
    void loadingExistingDataDoesNotOverwriteIt() throws Exception {
        StorageService storage = storage();
        InventoryService inventory = new InventoryService(storage, new EventBus());
        inventory.add("Notebook", "Stationery", 3000, 8, 2, "");
        String saved = Files.readString(storage.path());

        assertEquals(1, new StorageService(storage.path()).load().products().size());
        assertEquals(saved, Files.readString(storage.path()));
    }

    @Test
    void emptyStartupDoesNotPublishProductOrStockEvents() throws Exception {
        List<AppEvent> events = new ArrayList<>();
        EventBus bus = new EventBus();
        bus.addListener(events::add);

        new InventoryService(storage(), bus);

        assertTrue(events.isEmpty());
    }

    @Test
    void malformedExistingFileIsNotOverwritten() throws Exception {
        StorageService storage = storage();
        Files.createDirectories(storage.path().getParent());
        Files.writeString(storage.path(), "{ invalid");

        assertThrows(Exception.class, storage::load);
        assertEquals("{ invalid", Files.readString(storage.path()));
    }
}
