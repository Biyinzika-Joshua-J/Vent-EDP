package com.inventorymonitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.inventorymonitor.model.Product;
import com.inventorymonitor.model.StockMovement;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

public class StorageService {
    public record Data(List<Product> products, List<StockMovement> movements) {}

    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public StorageService(Path file) {
        this.file = file;
    }

    public Data load() throws IOException {
        if (Files.notExists(file)) {
            Data empty = new Data(List.of(), List.of());
            save(empty);
            return empty;
        }
        Data data = mapper.readValue(file.toFile(), Data.class);
        if (data == null || data.products() == null || data.movements() == null) {
            throw new IOException("Inventory file has missing data.");
        }
        return data;
    }

    public void save(Data data) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(file.toAbsolutePath().getParent(), "inventory-", ".tmp");
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), data);
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public Path path() {
        return file;
    }

}
