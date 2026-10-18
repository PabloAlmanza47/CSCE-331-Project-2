import javafx.fxml.FXML;
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
import javafx.scene.control.Menu;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Label;
import javafx.scene.control.Accordion;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Pair;
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


public class ManagerController {
    //Variables
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
    private static final int MAX_ITEM_STOCK = 99999999;
    private boolean controllerActive = true;
    @FXML BarChart<Number, String> salesGraph; //Sales graph display
    @FXML public ListView<MenuItem> menuListView; //Menu list creation
    @FXML public ListView<StockItem> stockListView; //Preserve remote stock editing through StockItem and StockListCell.
    @FXML BarChart<Number, String> stockGraph; //Stock/product used graph display
    @FXML Accordion receiptAccordion; // Receipt accordion display


    /**
     * This method runs automatically when the FXML loads
     * @author Ashley Hoang
     */
    @FXML
    public void initialize() {
        /*
         * // Set up what happens when button is clicked
         * queryButton.setOnAction(event -> runQuery());
         * closeButton.setOnAction(event -> closeWindow());
         */
        runQuery();
    }

    /**
     * Your method to run the database query
     * @author Pablo Almanza
     * @author Ashley Hoang
     */
    private void runQuery() {
        stockListView.setDisable(true);
        menuListView.setDisable(true);
        Task<ManagerData> task = new Task<>() {
            @Override
            protected ManagerData call() throws Exception {
                List<SalesRecord> sales = new ArrayList<>();
                List<StockRecord> stock = new ArrayList<>();
                List<StockGraphRecord> productsUsed = new ArrayList<>();
                List<ReceiptRecord> receipts = new ArrayList<>();
                List<MenuRecord> menu = new ArrayList<>();
                try (Connection conn = getConnection();
                        Statement stmt = conn.createStatement()) {
                    try (ResultSet resultSet = stmt.executeQuery(
                            "SELECT i.item_id, i.name AS itemNames, SUM(ri.quantity) AS numSold FROM receipt_item ri "
                                    + "JOIN item i ON i.item_id = ri.item_id GROUP BY i.name, i.item_id ORDER BY numSold DESC")) {
                        while (resultSet.next()) {
                            //Sales Graph
                            sales.add(new SalesRecord(resultSet.getInt("item_id"), resultSet.getString("itemNames"),
                                    resultSet.getLong("numSold")));
                        }
                    }
                    try (ResultSet resultSet = stmt.executeQuery(
                            "SELECT inventory_id, name, stock, min_stock, next_shipment, shelf_life "
                                    + "FROM inventory ORDER BY stock ASC, inventory_id ASC")) {
                        while (resultSet.next()) {
                            //Stock List
                            stock.add(new StockRecord(resultSet.getInt("inventory_id"),
                                    resultSet.getString("name"), resultSet.getInt("stock"),
                                    resultSet.getInt("min_stock"), resultSet.getString("next_shipment"),
                                    resultSet.getString("shelf_life")));
                        }
                    }
                    try (ResultSet resultSet = stmt.executeQuery(
                            "SELECT i.inventory_id, i.name AS inventoryNames, SUM(ri.quantity) AS stockUsed "
                            + "FROM receipt_item ri "
                            + "JOIN item it ON ri.item_id = it.item_id JOIN ingredient_list il ON it.item_id = il.item_id "
                            + "JOIN inventory i ON il.inventory_id = i.inventory_id JOIN receipt r ON ri.receipt_id = r.receipt_id "
                            + "GROUP BY i.inventory_id, i.name ORDER BY stockUsed DESC")) {
                        while (resultSet.next()) {
                            //Stock Graph
                            productsUsed.add(new StockGraphRecord(resultSet.getInt("inventory_id"), resultSet.getString("inventoryNames"),
                                    resultSet.getLong("stockUsed")));
                        }
                    }
                    try (ResultSet resultSet = stmt.executeQuery(
                            "SELECT r.receipt_id, i.item_id, i.name AS item_name, ri.quantity,  ri.price_at_sale, r.receipt_timestamp "
                                    + "FROM receipt r JOIN receipt_item ri ON r.receipt_id = ri.receipt_id "
                                    + "JOIN item i ON ri.item_id = i.item_id WHERE r.receipt_id IN "
                                    + "(SELECT receipt_id FROM receipt ORDER BY receipt_timestamp DESC, receipt_id DESC LIMIT 20) "
                                    + "ORDER BY r.receipt_timestamp DESC, r.receipt_id DESC, ri.receipt_item_id")) {
                        while (resultSet.next()) {
                            //Receipt Record
                            receipts.add(new ReceiptRecord(resultSet.getInt("receipt_id"), 
                                    resultSet.getInt("item_id"),
                                    resultSet.getString("item_name"), resultSet.getInt("quantity"),
                                    resultSet.getBigDecimal("price_at_sale"),
                                resultSet.getString("receipt_timestamp")));
                        }
                    }
                    try (ResultSet resultSet = stmt.executeQuery(
                            "SELECT item_id, name, price FROM item WHERE active = TRUE ORDER BY item_id")) {
                        while (resultSet.next()) {
                            //Menu List
                            menu.add(new MenuRecord(resultSet.getInt("item_id"),
                                    resultSet.getString("name"), resultSet.getDouble("price")));
                        }
                    }
                }
                return new ManagerData(sales, stock, productsUsed, receipts, menu);
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

    /**
     * Applies the manager data to the GUI components.
     * @param data The compiled manager data containing sales, stock, receipts, and menu information.
     * @author Pablo Almanza
    */
    private void applyManagerData(ManagerData data) {
        //Sales Graph
        salesGraph.getData().clear();
        XYChart.Series<Number, String> salesSeries = new XYChart.Series<>();
        for (SalesRecord sale : data.getSales()) {
            long numSold = sale.getQuantity();
            String name = sale.getName();
            int id = sale.getID();
            String label = "#" + id + "-" + name + ' ' + numSold;
            salesSeries.getData().add(new XYChart.Data<>(numSold, label));
        }
        salesGraph.getData().add(salesSeries);

        //Stock list view
        ObservableList<StockItem> stockItems = FXCollections.observableArrayList();
        for (StockRecord item : data.getStock()) {
            stockItems.add(new StockItem(item.getInventoryID(), item.getName(), item.getStock(), item.getMinStock(),
                    item.getNextShipment(), item.getShelfLife()));
        }
        stockListView.setItems(stockItems);
        stockListView.setCellFactory(lv -> new StockListCell(this));

        //product used/stock graph
        stockGraph.getData().clear();
        XYChart.Series<Number, String> stockSeries = new XYChart.Series<>();
        for (StockGraphRecord stock : data.getStockGraphRecord()) {
            long stockUsed = stock.getQuantity();
            String name = stock.getName();
            int id = stock.getID();
            String label = "#" + id + "-" + name + ' ' + stockUsed;
            stockSeries.getData().add(new XYChart.Data<>(stockUsed, label));
        }
        stockGraph.getData().add(stockSeries);

        //receipt accordion
        receiptAccordion.getPanes().clear();
        int currentReceiptID = -1;
        VBox itemList = null;
        BigDecimal receiptTotal = BigDecimal.ZERO;
        TitledPane receiptPane = null;
        for (ReceiptRecord receipt : data.getReceipts()) {
            String timestamp = receipt.getTimestamp();
            if (receipt.getReceiptID() != currentReceiptID) {
                if (receiptPane != null)
                    itemList.getChildren().add(new Label(String.format("Total: $%.2f\n\n%s", receiptTotal, timestamp)));
                currentReceiptID = receipt.getReceiptID();
                receiptTotal = BigDecimal.ZERO;
                itemList = new VBox(5);
                itemList.setPadding(new Insets(10));
                receiptPane = new TitledPane("Receipt " + receipt.getReceiptID(), itemList);
                receiptPane.setMaxWidth(Double.MAX_VALUE);
                receiptAccordion.getPanes().add(receiptPane);
            }
            int itemID = receipt.getItemID();
            String itemName = receipt.getItemName();
            int quantity = receipt.getQuantity();
            // Checkout stores price_at_sale as the complete line total.
            BigDecimal lineTotal = receipt.getLineTotal();
            if (lineTotal == null)
                lineTotal = BigDecimal.ZERO;
            receiptTotal = receiptTotal.add(lineTotal);
            itemList.getChildren().add(new Label(String.format("#%d %s    x%d    $%.2f", itemID, itemName, quantity, lineTotal)));
        }
        if (receiptPane != null) itemList.getChildren().add(new Label(
                String.format("Total: $%.2f", receiptTotal)));

        //menu list view
        ObservableList<MenuItem> menuItems = FXCollections.observableArrayList();
        for (MenuRecord item : data.getMenu()) {
            menuItems.add(new MenuItem(item.getItemID(), item.getName(), item.getPrice()));
        }
        menuListView.setItems(menuItems);
        menuListView.setCellFactory(lv -> new MenuListCell(this));
    }

    /*-------------------------------------------Sales Graph-----------------------------------------*/
    /**
     * Opens a dialog to allow the user to change the time period for the sales graph.
     * @param event The ActionEvent triggered when the user selects to change the time period.
     * @throws IllegalArgumentException if the user enters an invalid time period
     * @author Ashley Hoang
     */
    @FXML
    public void changeSoldTimePeriod(ActionEvent event) {
        // Create a dialog to get the new time period from the user
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Change Time Period");
        dialog.setHeaderText("Enter the start and end time periods (inclusive):");

        //Ask for time periods
        TextField startDateField = new TextField();
        startDateField.setPromptText("ex. 2026-01-31 00:00:00");
        TextField endDateField = new TextField();
        endDateField.setPromptText("ex. 2026-01-31 23:59:59");

        //Grid pane
        GridPane grid = new GridPane();
        grid.setVgap(10);
        grid.add(new Label("Start:"), 0, 0);
        grid.add(startDateField, 1, 0);
        grid.add(new Label("End:"), 0, 1);
        grid.add(endDateField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        //when button is clicked, gets the strings of the time
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return new Pair<>(startDateField.getText(), endDateField.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();
        if (result.isPresent()) {
            Pair<String, String> times = result.get();
            String newTimePeriod1 = times.getKey().trim();
            String newTimePeriod2 = times.getValue().trim();

            if (newTimePeriod1.isEmpty() || newTimePeriod2.isEmpty()) {
                showError("Invalid Input", "Both start and end time periods are required.", "");
                return;
            }

            try {
                // Update the stock graph with the new time period
                updateSalesGraph(newTimePeriod1, newTimePeriod2);
            } catch (IllegalArgumentException e) {
                showError("Invalid Input", "Please enter valid time periods.", e.getMessage());
            }
        }
    }

    /**
     * Updates the sales graph to display the number of items sold within the specified time period.
     * @param timePeriod1 The start time for which to display sales data.
     * @param timePeriod2 The end time for which to display sales data.
     * @author Ashley Hoang
     */
    private void updateSalesGraph(String timePeriod1, String timePeriod2) {
        Task<XYChart.Series<Number, String>> task = new Task<>() {
            @Override
            protected XYChart.Series<Number, String> call() throws Exception {
                XYChart.Series<Number, String> soldSeries = new XYChart.Series<>();
                try (Connection conn = getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(
                             "SELECT i.item_id, i.name AS itemNames, SUM(ri.quantity) AS numSold " +
                                     "FROM receipt_item ri " +
                                     "JOIN item i ON i.item_id = ri.item_id " +
                                     "JOIN receipt r ON r.receipt_id = ri.receipt_id " +
                                     "WHERE r.receipt_timestamp >= CAST(? AS timestamp) AND r.receipt_timestamp <= CAST(? AS timestamp) " +
                                     "GROUP BY i.name, i.item_id ORDER BY numSold DESC")) {
                    pstmt.setString(1, timePeriod1);
                    pstmt.setString(2, timePeriod2);
                    try (ResultSet resultSet = pstmt.executeQuery()) {
                        while (resultSet.next()) {
                            int numSold = resultSet.getInt("numSold");
                            String name = resultSet.getString("itemNames");
                            int id = resultSet.getInt("item_id");
                            String label = "#" + id + "-" + name + ' ' + numSold;
                            soldSeries.getData().add(new XYChart.Data<>(numSold, label));
                        }
                    }
                }
                return soldSeries;
            }
        };
        task.setOnSucceeded(event -> {
            salesGraph.setTitle("Items Sold in From " + timePeriod1 + " to " + timePeriod2);
            if (controllerActive) {
                salesGraph.getData().clear();
                salesGraph.getData().add(task.getValue());
            }
        });
        task.setOnFailed(event -> {
            if (controllerActive) {
                Throwable exception = task.getException();
                exception.printStackTrace();
                showError("Sales Graph Update Failed", "The sales graph could not be updated.", "Please enter valid dates.");
            }
        });
        if (!startTask(task)) {
            showError("Sales Graph Update Failed", "The database is busy.", "Please try again.");
        }
    }
    
    /*-------------------------------------------Stock List-----------------------------------------*/
    /**
     * Updates the stock of an inventory item in the database.
     *
     * @author Ashley Hoang
     * @return user's inputted change in stock
     * @throws SQLException             if there is an error with the database query
     * @throws IllegalArgumentException if there is an error with the list creation
     */
    public int inputNewStock() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Change Stock");
        dialog.setHeaderText("Enter the change in stock (add a '-' if decreasing stock): ");
        dialog.setContentText("Change in stock: ");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return 0;
        try {
            int stock = Integer.parseInt(result.get().trim());
            if ((MAX_ITEM_STOCK) < stock)
                throw new IllegalArgumentException();
            return stock;
        } catch (IllegalArgumentException e) {
            showDatabaseError("update stock", "Enter a stock from 0 to 99999999 with no decimal places.");
            return 0;
        }
    }
    
    /**
     * Updates the stock quantity of an item in the database.
     *
     * @author Ashley Hoang
     * @author Pablo Almanza
     * @param inventoryID The ID of the inventory item to update.
     * @param stockDelta  The change in stock quantity.
     * @throws SQLException             if there is an error with the database query
     * @throws IllegalArgumentException if there is an error with the list creation
     */
    public void updateStockInDatabase(int inventoryID, int stockDelta, Consumer<StockUpdateResult> callback) {
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

    /*-------------------------------------------Stock Graph-----------------------------------------*/
    /**
     * Opens a dialog to allow the user to change the time period for the stock used graph.
     * @param event The ActionEvent triggered when the user selects to change the time period.
     * @throws IllegalArgumentException if the user enters an invalid time period
     * @author Ashley Hoang
     */
    @FXML
    public void changeStockTimePeriod(ActionEvent event) {
        // Create a dialog to get the new time period from the user
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Change Time Period");
        dialog.setHeaderText("Enter the start and end time periods (inclusive):");

        //Ask for time periods
        TextField startDateField = new TextField();
        startDateField.setPromptText("ex. 2026-01-31 00:00:00");
        TextField endDateField = new TextField();
        endDateField.setPromptText("ex. 2026-01-31 23:59:59");

        //Grid pane
        GridPane grid = new GridPane();
        grid.setVgap(10);
        grid.add(new Label("Start:"), 0, 0);
        grid.add(startDateField, 1, 0);
        grid.add(new Label("End:"), 0, 1);
        grid.add(endDateField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        //when button is clicked, gets the strings of the time
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return new Pair<>(startDateField.getText(), endDateField.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();
        if (result.isPresent()) {
            Pair<String, String> times = result.get();
            String newTimePeriod1 = times.getKey().trim();
            String newTimePeriod2 = times.getValue().trim();

            if (newTimePeriod1.isEmpty() || newTimePeriod2.isEmpty()) {
                showError("Invalid Input", "Both start and end time periods are required.", "");
                return;
            }

            try {
                // Update the stock graph with the new time period
                updateStockGraph(newTimePeriod1, newTimePeriod2);
            } catch (IllegalArgumentException e) {
                showError("Invalid Input", "Please enter valid time periods.", e.getMessage());
            }
        }
    }
    /**
     * Updates the sales graph to display the number of items sold within the specified time period.
     * @param timePeriod1 The start time for which to display stock data.
     * @param timePeriod2 The end time for which to display stock data.
     * @author Ashley Hoang
     */
    private void updateStockGraph(String timePeriod1, String timePeriod2) {
        Task<XYChart.Series<Number, String>> task = new Task<>() {
            @Override
            protected XYChart.Series<Number, String> call() throws Exception {
                XYChart.Series<Number, String> stockSeries = new XYChart.Series<>();
                try (Connection conn = getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(
                            "SELECT i.inventory_id, i.name AS inventoryNames, SUM(ri.quantity) AS stockUsed "
                            + "FROM receipt_item ri "
                            + "JOIN item it ON ri.item_id = it.item_id JOIN ingredient_list il ON it.item_id = il.item_id "
                            + "JOIN inventory i ON il.inventory_id = i.inventory_id JOIN receipt r ON ri.receipt_id = r.receipt_id "
                            + "WHERE r.receipt_timestamp >= CAST(? AS timestamp) AND r.receipt_timestamp <= CAST(? AS timestamp) "
                            + "GROUP BY i.inventory_id, i.name ORDER BY stockUsed DESC")) {
                    pstmt.setString(1, timePeriod1);
                    pstmt.setString(2, timePeriod2);
                    try (ResultSet resultSet = pstmt.executeQuery()) {
                        while (resultSet.next()) {
                            int stockUsed = resultSet.getInt("stockUsed");
                            String name = resultSet.getString("inventoryNames");
                            int id = resultSet.getInt("inventory_id");
                            String label = "#" + id + "-" + name + ' ' + stockUsed;
                            stockSeries.getData().add(new XYChart.Data<>(stockUsed, label));
                        }
                    }
                }
                return stockSeries;
            }
        };
        task.setOnSucceeded(event -> {
            stockGraph.setTitle("Stock Used From " + timePeriod1 + " to " + timePeriod2);
            if (controllerActive) {
                stockGraph.getData().clear();
                stockGraph.getData().add(task.getValue());
            }
        });
        task.setOnFailed(event -> {
            if (controllerActive) {
                Throwable exception = task.getException();
                exception.printStackTrace();
                showError("Stock Graph Update Failed", "The stock graph could not be updated.", "Please enter valid dates.");
            }
        });
        if (!startTask(task)) {
            showError("Stock Graph Update Failed", "The database is busy.", "Please try again.");
        }
    }

    /*-------------------------------------------Menu List-----------------------------------------*/
    /**
     * Updates the price of an item in the database.
     *
     * @author Ashley Hoang
     * @return user's inputted price
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

    /**
     * Updates the price of an item in the database.
     * @param itemID The ID of the item in the item database
     * @param newPrice The new price of the item (computed by inputNewPrice)
     * @param callback A callback function that will be called with the result of the price update operation (double on success). 
     * @author Pablo Almanza
     */
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

    /**
     * Adds a new menu item to the database based on user input from a dialog.
     *
     * @author Ashley Hoang
     * @param event The ActionEvent triggered when the user clicks the "Add Menu Item" button.
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

    /**
     * Adds a new menu item to the database.
     * @param itemID The ID of the new menu item.
     * @param categoryID The category ID of the new menu item.
     * @param nutritionInfo The nutrition information of the new menu item.
     * @param itemPrice The price of the new menu item.
     * @param size The unit size of the new menu item.
     * @param itemName The name of the new menu item.
     * @param callback A callback function that will be called with the result of the add operation (true on success, false on failure).
     * @author Pablo Almanza
     */
    private void addMenuItemToDatabase(int itemID, int categoryID, String nutritionInfo,
            BigDecimal itemPrice, String size, String itemName, Consumer<Boolean> callback) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                try (Connection conn = getConnection();
                        PreparedStatement pstmt = conn.prepareStatement(
                                "INSERT INTO item (item_id, category_id, nutrition, price, unit_size, name, active) "
                                        + "VALUES (?, ?, ?, ?, ?, ?, TRUE)")) {
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
     * @author Pablo Almanza
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

    /**
     * Removes a menu item from the list view in the GUI.
     * @param item The menu item to remove from the list view.
     * @author Pablo Almanza
     */
    public void removeMenuItemFromList(MenuItem item) {
        if (menuListView != null) menuListView.getItems().remove(item);
    }
    
    /*-------------------------------------------Window-----------------------------------------*/
    /**
     * Closes the current window.
     * @param event The ActionEvent triggered when the user clicks the close button.
     * @author Ashley Hoang
     */
    @FXML
    public void closeWindow(ActionEvent event) {
        controllerActive = false;
        Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
        stage.close();
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
        stage.setTitle("Cashier View");
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
    
    /**
     * Starts a task in the database executor.
     * @param task The task to start.
     * @return true if the task was started successfully, false otherwise.
     * @author Pablo Almanza
     */
    private boolean startTask(Task<?> task) {
        try {
            DATABASE_EXECUTOR.submit(task);
            return true;
        } catch (RejectedExecutionException e) {
            return false;
        }
    }

    /**
     * Gets a connection to the database.
     * @return A Connection object representing the connection to the database.
     * @throws SQLException if there is an error with the database connection
     * @throws ClassNotFoundException if the PostgreSQL JDBC driver class is not found
     * @author Pablo Almanza
     */
    private Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("org.postgresql.Driver");
        DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SECONDS);
        Connection conn = DriverManager.getConnection(DB_URL, DbSetup.user, DbSetup.pswd);
        conn.setNetworkTimeout(Runnable::run, NETWORK_TIMEOUT_MILLISECONDS);
        return conn;
    }

    /**
     * Shows a database error message in an alert dialog.
     * @param operation The operation that failed (e.g., "update stock", "add menu item").
     * @param details Additional details about the error (e.g., "The stock value changed. Current stock: 5").
     * @author Pablo Almanza
     */
    private void showDatabaseError(String operation, String details) {
        showError("Database Error", "Unable to " + operation, details);
    }

    /**
     * Shows a database error message in an alert dialog when the stock update fails due to a stock value change.
     * @param currentStock The current stock value in the database.
     * @author Pablo Almanza
     */
    public void showStockUpdateFailure(int currentStock) {
        showDatabaseError("update stock", "The stock value changed. Current stock: " + currentStock);
    }

    /**
     * Shows a database error message in an alert dialog when the inventory record no longer exists.
     * @author Pablo Almanza
     */
    public void showMissingInventoryError() {
        showDatabaseError("update stock", "The inventory record no longer exists.");
    }

    /**
     * Displays an error alert dialog with the specified title, header, and details.
     * @param title The title of the error dialog.
     * @param header The header of the error dialog.
     * @param details Additional details about the error.
     * @author Pablo Almanza
     */
    private void showError(String title, String header, String details) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(details);
        alert.showAndWait();
    }

    /**
     * Checks whether or not the Manager Controller is currently active
     * @return if the Manager Controller is currently active
     * @author Pablo ALmanza
     */
    public boolean isActive() {
        return controllerActive;
    }
}
