# Frontend and Backend Split Design

## Goal

Make the JavaFX application visibly and actually separated into frontend and backend code while preserving the current cashier, manager, PostgreSQL, and launch behavior.

## Current problem

The JavaFX controllers currently contain both UI code and PostgreSQL connection/SQL code. The Java files also live in one default package under `JavaFXGUI/`, while the older Swing/JDBC demo lives separately under `GUI/`. This makes it difficult to tell which code renders screens and which code talks to the database.

## Design

Use explicit Java packages and a source/resource layout:

```text
src/
├── frontend/
│   ├── app/                    # JavaFX entry point
│   ├── controllers/            # FXML event handling and screen state
│   ├── components/             # JavaFX ListCell implementations
│   └── resources/              # FXML and CSS
└── backend/
    ├── config/                 # Database credentials/configuration boundary
    ├── database/               # Connections and SQL repositories
    └── models/                 # Data returned from or sent to the database
```

The frontend may depend on backend models and repository interfaces/classes, but backend code must not import JavaFX classes. Controllers keep JavaFX `Task` scheduling and user-facing alerts; repository methods perform synchronous database work and return typed results. This is the smallest practical split because the existing UI already owns asynchronous task and error-display behavior.

### Frontend files

- `frontend.app.DatabaseApp`: launches JavaFX and loads the cashier FXML resource.
- `frontend.controllers.CashierController`: cashier interaction, cart state, background task coordination, and alerts. It calls `CashierRepository` instead of opening JDBC connections or writing SQL.
- `frontend.controllers.ManagerController`: manager interaction, charts, dialogs, background task coordination, and alerts. It calls `ManagerRepository` instead of opening JDBC connections or writing SQL.
- `frontend.components.MenuListCell` and `frontend.components.StockListCell`: JavaFX list-cell rendering and controller callbacks.
- `frontend.resources`: `cashierGUI.fxml`, `managerGUI.fxml`, `cashierStyle.css`, and `managerStyle.css`.

### Backend files

- `backend.config.DbSetup`: retains the existing database credential boundary and does not expose credentials in frontend code.
- `backend.database.DatabaseConnection`: creates PostgreSQL connections and applies the existing login/network timeout behavior.
- `backend.database.CashierRepository`: menu loading and transactional checkout operations currently embedded in `CashierController`.
- `backend.database.ManagerRepository`: manager dashboard queries, stock updates, menu updates, and report queries currently embedded in `ManagerController`.
- `backend.models`: existing record/data classes used as repository inputs and outputs (`MenuRecord`, `ReceiptRecord`, `SalesRecord`, `StockRecord`, `StockGraphRecord`, `XRecord`, `ManagerData`, `StockUpdateResult`, `MenuItem`, `StockItem`, `CartLine`, and `MealSize`). No behavior changes are intended in these classes.

The legacy `GUI/jdbcGUI.java` demo remains outside the JavaFX application and is not made part of the new runtime path. Its `DbSetup` reference is updated only if the shared configuration is moved; otherwise it remains a separate legacy example.

## Boundary rules

1. No `java.sql` imports or SQL string literals remain in frontend controllers/components.
2. No backend package imports JavaFX classes.
3. FXML `fx:controller` values use the new frontend controller package names.
4. Resource loading uses classpath resources rather than the old directory-relative assumptions.
5. `Run-Gui.sh` compiles all source files into `JavaFXGUI/out` (or the equivalent output directory), includes the PostgreSQL driver, and launches `frontend.app.DatabaseApp`.
6. Existing database transactions, validation, callbacks, error handling, and asynchronous UI behavior remain unchanged.
7. No new third-party dependencies are added.

## Verification

- Compile the complete application with the existing JavaFX SDK and PostgreSQL driver.
- Run the application when JavaFX is available and verify the cashier screen loads.
- Inspect source imports to confirm the boundary rules.
- Run `git diff --check` and preserve all pre-existing user changes, especially the modified `ManagerController.java` and documentation renames.

## Scope intentionally skipped

This change does not add a web API, authentication layer, dependency-injection framework, database migration, new UI behavior, or a separate process for the backend. Those would be unnecessary for the requested code/file-structure separation.
