package com.inventorymonitor.event;

import java.util.ArrayList;
import java.util.List;

public final class EventBus {
    private final List<EventListener> listeners = new ArrayList<>();
    public synchronized void addListener(EventListener listener) {
        listeners.add(listener);
    }
    public synchronized void removeListener(EventListener listener) {
        listeners.remove(listener);
    }

    public void publish(AppEvent event) {
        List<EventListener> current;

        synchronized (this) {
            current = new ArrayList<>(listeners);
        }

        for (EventListener listener : current) {
            try {
                listener.onEvent(event);
            } catch (RuntimeException error) {
                System.err.println("Event handler failed: " + error.getMessage());
            }
        }
    }
}
