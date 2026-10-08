import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
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

public class managerController {
    private static final String DB_URL = "jdbc:postgresql://csce-315-db.engr.tamu.edu/team1db"; // database location
    private static final BigDecimal MAX_ITEM_PRICE = new BigDecimal("99999999.99");

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
        try {
            dbSetup my = new dbSetup();
            Class.forName("org.postgresql.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, my.user, my.pswd);
                    Statement stmt = conn.createStatement()) {
                try (ResultSet items = stmt.executeQuery(
                        "SELECT i.name AS itemNames, SUM(ri.quantity) AS numSold FROM receipt_item ri "
                                + "JOIN item i ON i.item_id = ri.item_id GROUP BY i.name ORDER BY numSold")) {
                    createSalesBarGraph(items);
                }
                try (ResultSet stocklist = stmt.executeQuery(
                        "SELECT inventory_id, name, stock, min_stock, next_shipment, shelf_life "
                                + "FROM inventory ORDER BY inventory_id")) {
                    createStockList(stocklist);
                }
                try (ResultSet receipts = stmt.executeQuery(
                        "SELECT r.receipt_id, r.receipt_timestamp, ri.receipt_item_id, i.item_id, "
                                + "i.name AS item_name, ri.quantity, ri.price_at_sale FROM receipt r "
                                + "JOIN receipt_item ri ON r.receipt_id = ri.receipt_id "
                                + "JOIN item i ON ri.item_id = i.item_id WHERE r.receipt_id IN "
                                + "(SELECT receipt_id FROM receipt ORDER BY receipt_timestamp DESC, receipt_id DESC LIMIT 10) "
                                + "ORDER BY r.receipt_timestamp DESC, r.receipt_id DESC, ri.receipt_item_id")) {
                    createReceiptList(receipts);
                }
                try (ResultSet menuList = stmt.executeQuery(
                        "SELECT item_id, name, price FROM item WHERE active = TRUE ORDER BY item_id")) {
                    createMenuList(menuList);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void closeWindow(ActionEvent event) {
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
    public StockUpdateResult updateStockInDatabase(int inventoryID, int stockDelta) {
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
                        int stock = resultSet.getInt("stock");
                        return new StockUpdateResult(stock, false, false);
                    }
                }
            }
            return new StockUpdateResult(null, false, true);
        } catch (SQLException | ClassNotFoundException e) {
            showDatabaseError("update stock", "The database update failed.");
        } catch (IllegalArgumentException e) {
            showDatabaseError("update stock", "The stock value is invalid.");
        }
        return new StockUpdateResult(null, false, false);
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
                int receiptID = receipts.getInt("receipt_id");
                if (receiptID != currentReceiptID) {
                    if (receiptPane != null)
                        itemList.getChildren().add(new Label(
                                String.format("Total: $%.2f", receiptTotal)));
                    currentReceiptID = receiptID;
                    receiptTotal = BigDecimal.ZERO;
                    itemList = new VBox(5);
                    itemList.setPadding(new Insets(10));
                    receiptPane = new TitledPane("Receipt " + receiptID, itemList);
                    receiptPane.setMaxWidth(Double.MAX_VALUE);
                    receiptAccordion.getPanes().add(receiptPane);
                }
                String itemName = receipts.getString("item_name");
                int quantity = receipts.getInt("quantity");
                // Checkout stores price_at_sale as the complete line total.
                BigDecimal lineTotal = receipts.getBigDecimal("price_at_sale");
                if (lineTotal == null)
                    lineTotal = BigDecimal.ZERO;
                receiptTotal = receiptTotal.add(lineTotal);
                itemList.getChildren().add(new Label(String.format(
                        "%s    x%d    $%.2f", itemName, quantity, lineTotal)));
            }
            if (receiptPane != null)
                itemList.getChildren().add(new Label(
                        String.format("Total: $%.2f", receiptTotal)));
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
    public Double updatePriceInDatabase(int itemID) {
        double newPrice = inputNewPrice();
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE item SET price = ? "
                                + "WHERE item_id = ? AND ? >= 0 RETURNING price")) {
            BigDecimal price = BigDecimal.valueOf(newPrice);
            pstmt.setBigDecimal(1, price);
            pstmt.setInt(2, itemID);
            pstmt.setBigDecimal(3, price);
            try (ResultSet resultSet = pstmt.executeQuery()) {
                if (resultSet.next()) return resultSet.getBigDecimal("price").doubleValue();
            }
            showDatabaseError("update price", "The price value could not be updated.");
        } catch (SQLException | ClassNotFoundException e) {
            showDatabaseError("update price", "The database update failed.");
        } catch (IllegalArgumentException e) {
            showDatabaseError("update price", "The price value is invalid.");
        }
        return null;
    }

    public double inputNewPrice() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Change Price");
        dialog.setHeaderText("Enter the new price: ");
        dialog.setContentText("Price: ");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                return Double.parseDouble(result.get());
            } catch (NumberFormatException e) {
                showDatabaseError("update price", "Invalid input. Please enter a valid number.");
            }
        }
        return 0.0; // Default to no change if input is invalid or canceled
    }

    private Connection getConnection() throws SQLException, ClassNotFoundException {
        dbSetup my = new dbSetup();
        Class.forName("org.postgresql.Driver");
        return DriverManager.getConnection(DB_URL, my.user, my.pswd);
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
                    pstmt.executeUpdate();
                }
                reloadApplication();
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
     * Removes a menu item from the database.
     *
     * @param item The menu item to remove.
     * @author Ashley Hoang
     * @throws SQLException             if there is an error with the database query
     * @throws IllegalArgumentException if there is an error with the list creation
     */
    public boolean removeMenuItemFromDatabase(MenuItem item) {
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE item SET active = FALSE WHERE item_id = ? AND active = TRUE")) {
            pstmt.setInt(1, item.getItemID());
            if (pstmt.executeUpdate() > 0) {
                menuListView.getItems().remove(item);
                return true;
            }
            showDatabaseError("remove menu item", "The menu item was not active.");
        } catch (SQLException | ClassNotFoundException e) {
            showDatabaseError("remove menu item", "The database update failed.");
        } catch (IllegalArgumentException e) {
            showDatabaseError("remove menu item", "The menu item is invalid.");
        }
        return false;
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
