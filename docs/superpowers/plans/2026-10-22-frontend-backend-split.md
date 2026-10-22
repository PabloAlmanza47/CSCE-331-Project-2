# Frontend and Backend Split Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Separate the JavaFX UI and PostgreSQL code into clearly named frontend and backend packages while preserving the current application behavior.

**Architecture:** Move UI entry point, controllers, JavaFX cells, FXML, and CSS under `src/frontend`. Move database configuration, connection creation, JDBC repositories, and database-facing records under `src/backend`. Controllers retain JavaFX background-task and alert coordination but delegate SQL and connection work to repositories.

**Tech Stack:** Java, JavaFX 25, PostgreSQL JDBC driver, Bash launcher.

**Spec:** `docs/superpowers/specs/2026-10-22-frontend-backend-split-design.md`

## Global Constraints

- No new third-party dependencies.
- Preserve existing database transactions, validation, callbacks, error handling, and asynchronous UI behavior.
- Backend packages must not import JavaFX.
- Frontend controllers/components must not import `java.sql` or contain SQL literals.
- Preserve unrelated pre-existing worktree changes.

## Review Focus

- FXML controller/resource paths: startup must load the cashier screen.
- Checkout transaction failures: inventory and receipt writes must retain existing rollback behavior.
- Manager background tasks: database work must remain off the JavaFX thread.
- Legacy `GUI/` demo: it must not be accidentally included in the JavaFX runtime.
- Credentials: database configuration must remain outside frontend packages.

### Task 1: Relocate sources and resources into explicit packages

**Files:**
- Create/move: `src/frontend/**`, `src/backend/**`
- Modify: FXML controller and resource references, `Run-Gui.sh`, `JavaFXGUI/README.md`

**Interfaces:**
- Produces package-qualified frontend entry point and controllers plus backend model/config packages for Task 2.

- [ ] Add package declarations and imports while preserving class APIs.
- [ ] Move FXML/CSS into frontend resources and update `fx:controller` and resource loading.
- [ ] Update the launcher to compile all sources and launch `frontend.app.DatabaseApp`.
- [ ] Verify source paths and package declarations with `rg`.

### Task 2: Extract JDBC access into backend repositories

**Files:**
- Create: `src/backend/database/DatabaseConnection.java`, `CashierRepository.java`, `ManagerRepository.java`
- Modify: `src/frontend/controllers/CashierController.java`, `ManagerController.java`
- Move: database-facing records into `src/backend/models/`

**Interfaces:**
- `DatabaseConnection.open() -> Connection`
- `CashierRepository.loadMenu() -> List<MenuRecord>`
- `CashierRepository.checkout(int locationId, int userId, TreeMap<Integer,Integer> quantities, TreeMap<Integer,BigDecimal> prices) -> CheckoutResult`
- `ManagerRepository.loadDashboard() -> ManagerData`
- Manager repository methods return the existing typed result records for stock, menu, sales, and report operations.

- [ ] Move SQL and JDBC resource management from both controllers into repositories.
- [ ] Keep controller `Task` callbacks and user-facing error handling unchanged.
- [ ] Remove controller JDBC imports, SQL literals, and connection helpers.
- [ ] Add a small source-level boundary check for JavaFX imports in backend and SQL imports in frontend controllers.

### Task 3: Verify and publish

**Files:**
- Modify only files required by verification findings.

- [ ] Run the launcher/build with the available JavaFX SDK and PostgreSQL driver.
- [ ] Run `git diff --check` and inspect the final diff for unrelated changes.
- [ ] Commit, push the branch, and open a GitHub PR for review.
