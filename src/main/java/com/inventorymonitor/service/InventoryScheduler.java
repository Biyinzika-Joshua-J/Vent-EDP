package com.inventorymonitor.service;

import com.inventorymonitor.event.EventBus;
import com.inventorymonitor.event.Events;

import java.time.LocalDateTime;
import java.util.concurrent.*;

public final class InventoryScheduler implements AutoCloseable {
    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(
                    r -> {
                        Thread thread = new Thread(r, "inventory-review");
                        thread.setDaemon(true);
                        return thread;
                    });

    public InventoryScheduler(EventBus bus) {
        // Reviews run off the JavaFX thread so the interface remains responsive.
        executor.scheduleAtFixedRate(
                () -> bus.publish(new Events.InventoryReview(LocalDateTime.now())), 60, 60, TimeUnit.SECONDS);
    }

    public void afterSeconds(Runnable action, int seconds) {
        executor.schedule(action, seconds, TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
