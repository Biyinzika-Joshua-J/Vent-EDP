# Vent

Vent is a JavaFX inventory application for Event-Driven Programming coursework. It stores products and stock movements in local JSON and has three event sources: GUI actions, a scheduled review, and supplier files.

## Run

Use Java 21 or newer and Maven 3.9 or newer. From this directory:

```text
mvn test
mvn javafx:run
```

On first start, `data/inventory.json` is created with no products or stock movements. Add products through the application. Later starts load the saved file without replacing it. Keep a copy of `data/inventory.json` if you need to preserve your own data. The app uses local storage; optional product image URLs require internet access.

## Screens and dialogs

- **Dashboard:** product counts, stock warnings, and the five most recent events.
- **Products:** filter, edit, and delete products. All matching products appear in the table. **Add Product** is in the page header. Add and Edit use the same `ProductDialog`.
- **Stock Movements:** the visible inline form is for quick entry. Pressing Enter in its quantity field also records the movement. With a product selected, **Ctrl+I** opens the Stock In dialog and **Ctrl+O** opens the Stock Out dialog for a guided entry. Both routes call the same `StockMovementsView.saveMovement` method. The table shows history.
- Deleting a product uses a JavaFX confirmation dialog. Invalid input and file errors use styled alerts.

## Try the three event sources

1. **GUI:** add a product with initial quantity 12 and low-stock threshold 10. Record Stock Out of 4 units to cross into Low Stock; take the remaining 8 out to reach Out of Stock.
2. **Scheduled:** leave the app open for 60 seconds. The Dashboard Recent Events card shows **Inventory review**.
3. **Supplier file:** first add a product. On a fresh installation, the first product has ID 1. Copy `samples/supplier-delivery.txt` into `incoming/`. The file contains `1,5`, meaning five units for product ID 1. If you already have saved products, edit the file to use an ID present in `data/inventory.json`. The stock increases and the file moves to `processed/`. To repeat, copy the sample again under a new `.txt` filename.

Invalid supplier files remain in `incoming/` and print a short error to the console. Product IDs must exist, and quantities must be positive whole numbers.

`samples/` is only a safe place to keep a demo file. The watcher reads `.txt` files in `incoming/`, not `samples/`, so the example does not change stock until you copy it. The coursework brief requires an external event source; it does not require this particular folder or file format.

## Source guide

| File | Responsibility |
|---|---|
| `Main.java` | Starts JavaFX and creates the application context before the UI thread starts. |
| `model/Product.java` | Product fields and stock status. |
| `model/StockMovement.java` | Saved history entry for a stock change. |
| `event/AppEvent.java` | Common marker for application events. |
| `event/Events.java` | Six small event records. |
| `event/EventListener.java` | One method: `onEvent`. |
| `event/EventBus.java` | Adds, removes, and calls listeners. |
| `event/EventLog.java` | Keeps the latest 100 events for the Dashboard. |
| `service/InventoryService.java` | Validates commands, saves changes, publishes events and stock warnings. |
| `service/StorageService.java` | Reads and safely replaces the JSON file; creates empty first-run data. |
| `service/InventoryScheduler.java` | Publishes a review every 60 seconds. |
| `service/SupplierFileWatcher.java` | Watches `incoming/`, records deliveries, and moves files to `processed/`. |
| `ui/AppContext.java` | Holds shared objects and one worker for inventory file writes. |
| `ui/MainLayout.java` | Sidebar, navigation, header, and warning toasts. |
| `ui/DashboardView.java` | Dashboard cards, alert list, and event list. |
| `ui/ProductsView.java` | Product table, filters, edit and delete actions. |
| `ui/StockMovementsView.java` | Inline stock form and movement history. |
| `ui/dialogs/ProductDialog.java` | Collects Add and Edit Product input. |
| `ui/dialogs/StockMovementDialog.java` | Collects Stock In and Stock Out input. |
| `ui/Ui.java` | Small shared JavaFX styling helpers. |
| `ui/PriceFormat.java` | Formats and parses UGX prices. |

The stylesheet is `src/main/resources/styles.css`. The original colors, cards, icons, tables, and forms are retained. Page and toast animations were removed.

See [EVENT_DRIVEN_DESIGN.md](EVENT_DRIVEN_DESIGN.md) for the full event catalogue, class and sequence diagrams, coursework mapping, and test plan. [report.md](report.md) describes the current implementation and the evidence still needed for submission. The existing `.docx` report files have not been regenerated from it.
