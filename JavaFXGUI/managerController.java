import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.BarChart;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Label;
import javafx.scene.control.Accordion;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import java.util.Optional;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.TextInputDialog;
import javafx.concurrent.Task;
import java.util.function.Consumer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;

public class managerController {
    private static final String DB_URL = "jdbc:postgresql://csce-315-db.engr.tamu.edu/team1db"; // database location
    private static final int LOGIN_TIMEOUT_SECONDS = 15;
    private static final int NETWORK_TIMEOUT_MILLISECONDS = 30000;
    private static final ExecutorService DATABASE_EXECUTOR = new ThreadPoolExecutor(
            4, 4, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(16),
            runnable -> {
                Thread thread = new Thread(runnable, "manager-database-worker");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());
    private static final BigDecimal MAX_ITEM_PRICE = new BigDecimal("99999999.99");
    private boolean controllerActive = true;

    public static final class StockUpdateResult {
        private final Integer stock;
        private final boolean updated;
        private final boolean missing;

        private StockUpdateResult(Integer stock, boolean updated, boolean missing) {
            this.stock = stock;
            this.updated = updated;
            this.missing = missing;
        }

        public Integer getStock() {
            return stock;
        }

        public boolean isUpdated() {
            return updated;
        }

        public boolean isMissing() {
            return missing;
        }
    }

    private static final class ManagerData {
        private final List<SalesRecord> sales;
        private final List<StockRecord> stock;
        private final List<ReceiptRecord> receipts;
        private final List<MenuRecord> menu;

        private ManagerData(List<SalesRecord> sales, List<StockRecord> stock,
                List<ReceiptRecord> receipts, List<MenuRecord> menu) {
            this.sales = sales;
            this.stock = stock;
            this.receipts = receipts;
            this.menu = menu;
        }
    }

    private static final class SalesRecord {
        private final String name;
        private final long quantity;

        private SalesRecord(String name, long quantity) {
            this.name = name;
            this.quantity = quantity;
        }
    }

    private static final class StockRecord {
        private final int inventoryID;
        private final String name;
        private final int stock;
        private final int minStock;
        private final String nextShipment;
        private final String shelfLife;

        private StockRecord(int inventoryID, String name, int stock, int minStock,
                String nextShipment, String shelfLife) {
            this.inventoryID = inventoryID;
            this.name = name;
            this.stock = stock;
            this.minStock = minStock;
            this.nextShipment = nextShipment;
            this.shelfLife = shelfLife;
        }
    }

    private static final class ReceiptRecord {
        private final int receiptID;
        private final String itemName;
        private final int quantity;
        private final BigDecimal lineTotal;

        private ReceiptRecord(int receiptID, String itemName, int quantity, BigDecimal lineTotal) {
            this.receiptID = receiptID;
            this.itemName = itemName;
            this.quantity = quantity;
            this.lineTotal = lineTotal;
        }
    }

    private static final class MenuRecord {
        private final int itemID;
        private final String name;
        private final double price;

        private MenuRecord(int itemID, String name, double price) {
            this.itemID = itemID;
            this.name = name;
            this.price = price;
        }
    }

    // This method runs automatically when the FXML loads
    @FXML
    public void initialize() {
        /*
         * // Set up what happens when button is clicked
         * queryButton.setOnAction(event -> runQuery());
         * closeButton.setOnAction(event -> closeWindow());
         */
        runQuery();
    }

    // Your method to run the database query
    private void runQuery() {
        stockListView.setDisable(true);
        menuListView.setDisable(true);
        Task<ManagerData> task = new Task<>() {
            @Override
            protected ManagerData call() throws Exception {
                List<SalesRecord> sales = new ArrayList<>();
                List<StockRecord> stock = new ArrayList<>();
                List<ReceiptRecord> receipts = new ArrayList<>();
                List<MenuRecord> menu = new ArrayList<>();
                try (Connection conn = getConnection();
                        Statement stmt = conn.createStatement()) {
                    try (ResultSet resultSet = stmt.executeQuery(
                            "SELECT i.name AS itemNames, SUM(ri.quantity) AS numSold FROM receipt_item ri "
                                    + "JOIN item i ON i.item_id = ri.item_id GROUP BY i.name ORDER BY numSold")) {
                        while (resultSet.next()) {
                            sales.add(new SalesRecord(resultSet.getString("itemNames"),
                                    resultSet.getLong("numSold")));
                        }
                    }
                    try (ResultSet resultSet = stmt.executeQuery(
                            "SELECT inventory_id, name, stock, min_stock, next_shipment, shelf_life "
                                    + "FROM inventory ORDER BY inventory_id")) {
                        while (resultSet.next()) {
                            stock.add(new StockRecord(resultSet.getInt("inventory_id"),
                                    resultSet.getString("name"), resultSet.getInt("stock"),
                                    resultSet.getInt("min_stock"), resultSet.getString("next_shipment"),
                                    resultSet.getString("shelf_life")));
                        }
                    }
                    try (ResultSet resultSet = stmt.executeQuery(
                            "SELECT r.receipt_id, i.name AS item_name, ri.quantity, ri.price_at_sale "
                                    + "FROM receipt r JOIN receipt_item ri ON r.receipt_id = ri.receipt_id "
                                    + "JOIN item i ON ri.item_id = i.item_id WHERE r.receipt_id IN "
                                    + "(SELECT receipt_id FROM receipt ORDER BY receipt_timestamp DESC, receipt_id DESC LIMIT 10) "
                                    + "ORDER BY r.receipt_timestamp DESC, r.receipt_id DESC, ri.receipt_item_id")) {
                        while (resultSet.next()) {
                            receipts.add(new ReceiptRecord(resultSet.getInt("receipt_id"),
                                    resultSet.getString("item_name"), resultSet.getInt("quantity"),
                                    resultSet.getBigDecimal("price_at_sale")));
                        }
                    }
                    try (ResultSet resultSet = stmt.executeQuery(
                            "SELECT item_id, name, price FROM item WHERE active = TRUE ORDER BY item_id")) {
                        while (resultSet.next()) {
                            menu.add(new MenuRecord(resultSet.getInt("item_id"),
                                    resultSet.getString("name"), resultSet.getDouble("price")));
                        }
                    }
                }
                return new ManagerData(sales, stock, receipts, menu);
            }
        };
        task.setOnSucceeded(event -> {
            if (controllerActive) {
                applyManagerData(task.getValue());
                stockListView.setDisable(false);
                menuListView.setDisable(false);
            }
        });
        task.setOnFailed(event -> {
            if (controllerActive) {
                stockListView.setDisable(false);
                menuListView.setDisable(false);
                showError("Manager Load Failed", "The manager data could not be loaded.",
                        "Please check the database connection and try again.");
            }
        });
        if (!startTask(task)) {
            stockListView.setDisable(false);
            menuListView.setDisable(false);
            showError("Manager Load Failed", "The database is busy.", "Please try again.");
        }
    }

    private void applyManagerData(ManagerData data) {
        salesGraph.getData().clear();
        XYChart.Series<Number, String> salesSeries = new XYChart.Series<>();
        for (SalesRecord sale : data.sales) {
            salesSeries.getData().add(new XYChart.Data<>(sale.quantity, sale.name));
        }
        salesGraph.getData().add(salesSeries);

        ObservableList<StockItem> stockItems = FXCollections.observableArrayList();
        for (StockRecord item : data.stock) {
            stockItems.add(new StockItem(item.inventoryID, item.name, item.stock, item.minStock,
                    item.nextShipment, item.shelfLife));
        }
        stockListView.setItems(stockItems);
        stockListView.setCellFactory(lv -> new StockListCell(this));

        receiptAccordion.getPanes().clear();
        int currentReceiptID = -1;
        VBox itemList = null;
        BigDecimal receiptTotal = BigDecimal.ZERO;
        TitledPane receiptPane = null;
        for (ReceiptRecord receipt : data.receipts) {
            if (receipt.receiptID != currentReceiptID) {
                if (receiptPane != null) itemList.getChildren().add(new Label(
                        String.format("Total: $%.2f", receiptTotal)));
                currentReceiptID = receipt.receiptID;
                receiptTotal = BigDecimal.ZERO;
                itemList = new VBox(5);
                itemList.setPadding(new Insets(10));
                receiptPane = new TitledPane("Receipt " + receipt.receiptID, itemList);
                receiptPane.setMaxWidth(Double.MAX_VALUE);
                receiptAccordion.getPanes().add(receiptPane);
            }
            BigDecimal lineTotal = receipt.lineTotal == null ? BigDecimal.ZERO : receipt.lineTotal;
            receiptTotal = receiptTotal.add(lineTotal);
            itemList.getChildren().add(new Label(String.format(
                    "%s    x%d    $%.2f", receipt.itemName, receipt.quantity, lineTotal)));
        }
        if (receiptPane != null) itemList.getChildren().add(new Label(
                String.format("Total: $%.2f", receiptTotal)));

        ObservableList<MenuItem> menuItems = FXCollections.observableArrayList();
        for (MenuRecord item : data.menu) {
            menuItems.add(new MenuItem(item.itemID, item.name, item.price));
        }
        menuListView.setItems(menuItems);
        menuListView.setCellFactory(lv -> new MenuListCell(this));
    }

    @FXML
    public void closeWindow(ActionEvent event) {
        controllerActive = false;
        Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
        stage.close();
    }

    @FXML
    BarChart<Number, String> salesGraph;

    @FXML
    public void createSalesBarGraph(ResultSet items) {
        try {
            XYChart.Series<Number, String> soldSeries = new XYChart.Series<>();
            while (items.next()) {
                soldSeries.getData().add(new XYChart.Data<>(items.getInt("numSold"), items.getString("itemNames")));
            }
            salesGraph.getData().add(soldSeries);
        } catch (SQLException | IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    // Preserve remote stock editing through StockItem and StockListCell.
    @FXML
    public ListView<StockItem> stockListView;

    @FXML
    public void createStockList(ResultSet stockList) {
        try {
            ObservableList<StockItem> stockItems = FXCollections.observableArrayList();
            while (stockList.next()) {
                stockItems.add(new StockItem(stockList.getInt("inventory_id"), stockList.getString("name"),
                        stockList.getInt("stock"), stockList.getInt("min_stock"),
                        stockList.getString("next_shipment"), stockList.getString("shelf_life")));
            }
            stockListView.setItems(stockItems);
            stockListView.setCellFactory(lv -> new StockListCell(this));
        } catch (SQLException | IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates the stock quantity of an item in the database.
     *
     * @author Ashley Hoang
     * @param inventoryID The ID of the inventory item to update.
     * @param stockDelta  The change in stock quantity.
     * @throws SQLException             if there is an error with the database query
     * @throws IllegalArgumentException if there is an error with the list creation
     */
    public void updateStockInDatabase(int inventoryID, int stockDelta,
            Consumer<StockUpdateResult> callback) {
        Task<StockUpdateResult> task = new Task<>() {
            @Override
            protected StockUpdateResult call() throws Exception {
                try (Connection conn = getConnection();
                        PreparedStatement pstmt = conn.prepareStatement(
                                "UPDATE inventory SET stock = stock + ? "
                                        + "WHERE inventory_id = ? AND stock + ? >= 0 RETURNING stock")) {
                    pstmt.setInt(1, stockDelta);
                    pstmt.setInt(2, inventoryID);
                    pstmt.setInt(3, stockDelta);
                    try (ResultSet resultSet = pstmt.executeQuery()) {
                        if (resultSet.next()) return new StockUpdateResult(resultSet.getInt("stock"), true, false);
                    }
                    try (PreparedStatement currentStock = conn.prepareStatement(
                            "SELECT stock FROM inventory WHERE inventory_id = ?")) {
                        currentStock.setInt(1, inventoryID);
                        try (ResultSet resultSet = currentStock.executeQuery()) {
                            if (resultSet.next()) {
                                return new StockUpdateResult(resultSet.getInt("stock"), false, false);
                            }
                        }
                    }
                    return new StockUpdateResult(null, false, true);
                }
            }
        };
        task.setOnSucceeded(event -> callback.accept(task.getValue()));
        task.setOnFailed(event -> {
            if (controllerActive) showDatabaseError("update stock", "The database update failed.");
            callback.accept(new StockUpdateResult(null, false, false));
        });
        if (!startTask(task)) {
            if (controllerActive) showDatabaseError("update stock", "The database is busy.");
            callback.accept(new StockUpdateResult(null, false, false));
        }
    }

    @FXML
    Accordion receiptAccordion;

    @FXML
    public void createReceiptList(ResultSet receipts) {
        try {
            int currentReceiptID = -1;
            VBox itemList = null;
            BigDecimal receiptTotal = BigDecimal.ZERO;
            TitledPane receiptPane = null;
            while (receipts.next()) {
                String timestamp = receipts.getString("receipt_timestamp");
                int receiptID = receipts.getInt("receipt_id");
                if (receiptID != currentReceiptID) {
                    if (receiptPane != null)
                        itemList.getChildren().add(new Label(String.format("Total: $%.2f\n\n%s", receiptTotal, timestamp)));
                    currentReceiptID = receiptID;
                    receiptTotal = BigDecimal.ZERO;
                    itemList = new VBox(5);
                    itemList.setPadding(new Insets(10));
                    receiptPane = new TitledPane("Receipt " + receiptID, itemList);
                    receiptPane.setMaxWidth(Double.MAX_VALUE);
                    receiptAccordion.getPanes().add(receiptPane);
                }
                int itemID = receipts.getInt("item_id");
                String itemName = receipts.getString("item_name");
                int quantity = receipts.getInt("quantity");
                // Checkout stores price_at_sale as the complete line total.
                BigDecimal lineTotal = receipts.getBigDecimal("price_at_sale");
                if (lineTotal == null)
                    lineTotal = BigDecimal.ZERO;
                receiptTotal = receiptTotal.add(lineTotal);
                itemList.getChildren().add(new Label(String.format("#%d %s    x%d    $%.2f", itemID, itemName, quantity, lineTotal)));
            }
            if (receiptPane != null)
                itemList.getChildren().add(new Label(String.format("Total: $%.2f", receiptTotal)));
        } catch (SQLException | IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    // Menu list creation
    @FXML
    public ListView<MenuItem> menuListView;

    @FXML
    public void createMenuList(ResultSet menuList) {
        try {
            ObservableList<MenuItem> menuItems = FXCollections.observableArrayList();
            while (menuList.next()) {
                int itemID = menuList.getInt("item_id");
                String itemName = menuList.getString("name");
                double itemPrice = menuList.getDouble("price");

                MenuItem menuItem = new MenuItem(itemID, itemName, itemPrice);
                menuItems.add(menuItem);
            }
            menuListView.setItems(menuItems);
            menuListView.setCellFactory(lv -> new MenuListCell(this));
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates the price of an item in the database.
     *
     * @author Ashley Hoang
     * @param itemID   The ID of the item to update.
     * @throws SQLException             if there is an error with the database query
     * @throws IllegalArgumentException if there is an error with the list creation
     */
    public BigDecimal inputNewPrice() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Change Price");
        dialog.setHeaderText("Enter the new price: ");
        dialog.setContentText("Price: ");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return null;
        try {
            BigDecimal price = new BigDecimal(result.get().trim());
            if (price.scale() > 2 || price.signum() < 0 || price.compareTo(MAX_ITEM_PRICE) > 0)
                throw new IllegalArgumentException();
            return price;
        } catch (IllegalArgumentException e) {
            showDatabaseError("update price", "Enter a price from 0.00 to 99999999.99 with at most two decimal places.");
            return null;
        }
    }

    public void updatePriceInDatabase(int itemID, BigDecimal newPrice, Consumer<Double> callback) {
        Task<Double> task = new Task<>() {
            @Override
            protected Double call() throws Exception {
                try (Connection conn = getConnection();
                        PreparedStatement pstmt = conn.prepareStatement(
                                "UPDATE item SET price = ? "
                                        + "WHERE item_id = ? AND ? >= 0 RETURNING price")) {
                    pstmt.setBigDecimal(1, newPrice);
                    pstmt.setInt(2, itemID);
                    pstmt.setBigDecimal(3, newPrice);
                    try (ResultSet resultSet = pstmt.executeQuery()) {
                        if (resultSet.next()) return resultSet.getBigDecimal("price").doubleValue();
                    }
                    return null;
                }
            }
        };
        task.setOnSucceeded(event -> {
            if (controllerActive && task.getValue() == null)
                showDatabaseError("update price", "The price value could not be updated.");
            callback.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            if (controllerActive) showDatabaseError("update price", "The database update failed.");
            callback.accept(null);
        });
        if (!startTask(task)) {
            if (controllerActive) showDatabaseError("update price", "The database is busy.");
            callback.accept(null);
        }
    }

    private boolean startTask(Task<?> task) {
        try {
            DATABASE_EXECUTOR.submit(task);
            return true;
        } catch (RejectedExecutionException e) {
            return false;
        }
    }

    private Connection getConnection() throws SQLException, ClassNotFoundException {
        dbSetup my = new dbSetup();
        Class.forName("org.postgresql.Driver");
        DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SECONDS);
        Connection conn = DriverManager.getConnection(DB_URL, my.user, my.pswd);
        conn.setNetworkTimeout(Runnable::run, NETWORK_TIMEOUT_MILLISECONDS);
        return conn;
    }

    private void showDatabaseError(String operation, String details) {
        showError("Database Error", "Unable to " + operation, details);
    }

    public void showStockUpdateFailure(int currentStock) {
        showDatabaseError("update stock", "The stock value changed. Current stock: " + currentStock);
    }

    public void showMissingInventoryError() {
        showDatabaseError("update stock", "The inventory record no longer exists.");
    }

    private void showError(String title, String header, String details) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(details);
        alert.showAndWait();
    }

    public boolean isActive() {
        return controllerActive;
    }

    /*
     * It is slow; do not use
     * /**
     * Checks if a menu item is in the database.
     *
     * @param itemID The ID of the item to check.
     *
     * @return true if the item is in the database, false otherwise.
     *
     * @author Ashley Hoang
     */
    /*
     * public boolean isMenuItemInDatabase(int itemID) {
     * try {
     * dbSetup my = new dbSetup();
     * Class.forName("org.postgresql.Driver");
     * Connection conn = DriverManager.getConnection(DB_URL, my.user, my.pswd);
     * String query = "SELECT * FROM item WHERE item_id = ?";
     * PreparedStatement pstmt = conn.prepareStatement(query);
     * pstmt.setInt(1, itemID);
     * ResultSet rs = pstmt.executeQuery();
     * boolean isInDatabase = rs.next();
     * rs.close();
     * pstmt.close();
     * conn.close();
     * return isInDatabase;
     * } catch (Exception e) {
     * e.printStackTrace();
     * return false;
     * }
     * }
     */
    /**
     * Adds a new menu item to the database based on user input from a dialog.
     *
     * @author Ashley Hoang
     * @param event
     * @throws SQLException             if there is an error with the database query
     * @throws IllegalArgumentException if there is an error with the list creation
     */
    @FXML
    public void addMenuItem(ActionEvent event) {
        // Get the input values from the user
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Menu Item");
        dialog.setHeaderText("Enter the details for the new menu item:");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        // Text fields for user input
        TextField itemIDField = new TextField();
        itemIDField.setPromptText("Item ID");
        TextField categoryIDField = new TextField();
        categoryIDField.setPromptText("Category ID");
        TextField nutritionInfoField = new TextField();
        nutritionInfoField.setPromptText("Nutrition Info");
        TextField itemPriceField = new TextField();
        itemPriceField.setPromptText("Item Price");
        TextField sizeField = new TextField();
        sizeField.setPromptText("Unit size");
        TextField itemNameField = new TextField();
        itemNameField.setPromptText("Item Name");

        grid.add(new Label("Item ID:"), 0, 0);
        grid.add(itemIDField, 1, 0);

        grid.add(new Label("Category ID:"), 0, 1);
        grid.add(categoryIDField, 1, 1);

        grid.add(new Label("Nutrition Info:"), 0, 2);
        grid.add(nutritionInfoField, 1, 2);

        grid.add(new Label("Item Price:"), 0, 3);
        grid.add(itemPriceField, 1, 3);

        grid.add(new Label("Unit Size:"), 0, 4);
        grid.add(sizeField, 1, 4);

        grid.add(new Label("Item Name:"), 0, 5);
        grid.add(itemNameField, 1, 5);

        dialog.getDialogPane().setContent(grid);

        // Show the dialog and wait for user input
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == addButtonType) {
            try {
                // Add the new menu item to the database
                int itemID = Integer.parseInt(itemIDField.getText());
                int categoryID = Integer.parseInt(categoryIDField.getText());
                String nutritionInfo = nutritionInfoField.getText();
                BigDecimal itemPrice = new BigDecimal(itemPriceField.getText().trim());
                if (itemPrice.scale() > 2 || itemPrice.signum() < 0
                        || itemPrice.compareTo(MAX_ITEM_PRICE) > 0)
                    throw new IllegalArgumentException("Invalid price");
                String size = sizeField.getText();
                String itemName = itemNameField.getText();
                Button addItemButton = (Button) event.getSource();
                addItemButton.setDisable(true);
                addMenuItemToDatabase(itemID, categoryID, nutritionInfo, itemPrice, size, itemName,
                        added -> {
                            if (controllerActive) {
                                addItemButton.setDisable(false);
                                if (added) reloadApplication();
                            }
                        });
            } catch (NumberFormatException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Input Error");
                alert.setHeaderText("Invalid Input");
                alert.setContentText("Please provide valid item, category, and price values.");
                alert.showAndWait();
            } catch (IllegalArgumentException e) {
                showDatabaseError("add menu item", "Please provide a price from 0.00 to 99999999.99 with at most two decimal places.");
            } catch (Exception e) {
                showDatabaseError("add menu item", "The menu item was not added.");
            }
        }
    }

    private void addMenuItemToDatabase(int itemID, int categoryID, String nutritionInfo,
            BigDecimal itemPrice, String size, String itemName, Consumer<Boolean> callback) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                try (Connection conn = getConnection();
                        PreparedStatement pstmt = conn.prepareStatement(
                                "INSERT INTO item (item_id, category_id, nutrition, price, unit_size, name, active) "
                                        + "VALUES (?, ?, ?, ?, ?, ?, FALSE)")) {
                    pstmt.setInt(1, itemID);
                    pstmt.setInt(2, categoryID);
                    pstmt.setString(3, nutritionInfo);
                    pstmt.setBigDecimal(4, itemPrice);
                    pstmt.setString(5, size);
                    pstmt.setString(6, itemName);
                    return pstmt.executeUpdate() == 1;
                }
            }
        };
        task.setOnSucceeded(event -> callback.accept(task.getValue()));
        task.setOnFailed(event -> {
            if (controllerActive) showDatabaseError("add menu item", "The menu item was not added.");
            callback.accept(false);
        });
        if (!startTask(task)) {
            if (controllerActive) showDatabaseError("add menu item", "The database is busy.");
            callback.accept(false);
        }
    }

    /**
     * Removes a menu item from the database.
     *
     * @param item The menu item to remove.
     * @author Ashley Hoang
     * @throws SQLException             if there is an error with the database query
     * @throws IllegalArgumentException if there is an error with the list creation
     */
    public void removeMenuItemFromDatabase(MenuItem item, Consumer<Boolean> callback) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                try (Connection conn = getConnection();
                        PreparedStatement pstmt = conn.prepareStatement(
                                "UPDATE item SET active = FALSE WHERE item_id = ? AND active = TRUE")) {
                    pstmt.setInt(1, item.getItemID());
                    return pstmt.executeUpdate() > 0;
                }
            }
        };
        task.setOnSucceeded(event -> {
            if (controllerActive && !task.getValue())
                showDatabaseError("remove menu item", "The menu item was not active.");
            callback.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            if (controllerActive) showDatabaseError("remove menu item", "The database update failed.");
            callback.accept(false);
        });
        if (!startTask(task)) {
            if (controllerActive) showDatabaseError("remove menu item", "The database is busy.");
            callback.accept(false);
        }
    }

    public void removeMenuItemFromList(MenuItem item) {
        if (menuListView != null) menuListView.getItems().remove(item);
    }

    /**
     * Changes the view to the cashier view when the corresponding button is
     * clicked.
     *
     * @author Ashley Hoang
     * @param event - the action event triggering the view change
     * @throws IOException if the FXML resource cannot be loaded
     */
    @FXML
    public void changeView(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("cashierGUI.fxml"));
        controllerActive = false;

        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1200, 800));
        stage.show();
    }

    /**
     * Reloads the current application view to reflect any changes made to the menu
     * items.
     *
     * @author Ashley Hoang
     * @throws IOException if the FXML resource cannot be loaded
     */
    @FXML
    public void reloadApplication() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("managerGUI.fxml"));
            Parent root = loader.load();
            controllerActive = false;
            Stage stage = (Stage) menuListView.getScene().getWindow();
            Scene scene = new Scene(root, 1200, 800);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showError("Reload Failed", "The manager view could not be refreshed.",
                    "Please try again or reopen the manager view.");
        }

    }
}
