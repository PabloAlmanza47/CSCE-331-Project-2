import java.sql.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;
import javafx.fxml.FXML;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.event.ActionEvent;

public class cashierController {
    @FXML private Button changeView, checkout;
    @FXML private VBox mainBox, sideBox, drinkBox, cartBox;
    private Label totalPriceLabel;
    @FXML private Button closeButton;
    private static final String DB_URL = "jdbc:postgresql://csce-315-db.engr.tamu.edu/team1db";
    private static final int LOGIN_TIMEOUT_SECONDS = 15;
    private static final int NETWORK_TIMEOUT_MILLISECONDS = 30000;
    private static final ExecutorService DATABASE_EXECUTOR = new ThreadPoolExecutor(
            4, 4, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(16),
            runnable -> {
                Thread thread = new Thread(runnable, "cashier-database-worker");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());
    private final HashMap<Integer, Integer> cart = new HashMap<>();
    private final HashMap<Integer, BigDecimal> cartPrices = new HashMap<>();
    private final HashMap<Integer, CheckBox> cartRows = new HashMap<>();
    private BigDecimal totalPrice = BigDecimal.ZERO;
    private int currentLocationId = 1;
    private int currentUserId = 3;
    private boolean checkoutInProgress;
    private boolean checkoutOutcomeUncertain;
    private boolean controllerActive = true;

    private static final class MenuRecord {
        private final int itemID;
        private final BigDecimal price;
        private final String name;
        private final int categoryID;

        private MenuRecord(int itemID, BigDecimal price, String name, int categoryID) {
            this.itemID = itemID;
            this.price = price;
            this.name = name;
            this.categoryID = categoryID;
        }
    }

    @FXML public void initialize() {
        runQuery();
        closeButton.setOnAction(event -> closeWindow());
        checkout.setOnAction(event -> confirmCheckout());
    }

    private void runQuery() {
        checkout.setDisable(true);
        Task<List<MenuRecord>> task = new Task<>() {
            @Override
            protected List<MenuRecord> call() throws Exception {
                List<MenuRecord> menu = new ArrayList<>();
                dbSetup my = new dbSetup();
                Class.forName("org.postgresql.Driver");
                try (Connection conn = openConnection(my.user, my.pswd);
                        PreparedStatement statement = conn.prepareStatement(
                                "SELECT name, price, item_id, category_id FROM item "
                                        + "WHERE category_id BETWEEN 1 AND 3 AND active = TRUE "
                                        + "ORDER BY category_id, item_id");
                        ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        menu.add(new MenuRecord(resultSet.getInt("item_id"),
                                resultSet.getBigDecimal("price"), resultSet.getString("name"),
                                resultSet.getInt("category_id")));
                    }
                }
                return menu;
            }
        };
        task.setOnSucceeded(event -> {
            if (!controllerActive) return;
            VBox[] areas = { mainBox, sideBox, drinkBox };
            for (VBox area : areas) area.getChildren().clear();
            if (totalPriceLabel == null) {
                totalPriceLabel = new Label("Total Price: 0.0$");
                cartBox.getChildren().add(totalPriceLabel);
            }
            for (MenuRecord record : task.getValue()) {
                VBox area = areas[record.categoryID - 1];
                Button button = new Button(record.name);
                button.setPrefSize(area.getPrefWidth(), 30);
                area.getChildren().add(button);
                button.setOnAction(action -> onButtonPressed(record.itemID, record.price, record.name));
            }
            checkout.setDisable(false);
        });
        task.setOnFailed(event -> {
            if (!controllerActive) return;
            checkout.setDisable(false);
            showError("Menu Load Failed", "The menu could not be loaded.",
                    "Please check the database connection and try again.");
        });
        if (!startTask(task)) {
            checkout.setDisable(false);
            showError("Menu Load Failed", "The database is busy.", "Please try again.");
        }
    }

    private void onButtonPressed(int itemID, BigDecimal price, String name) {
        if (!cart.containsKey(itemID)) {
            CheckBox checkBox = new CheckBox();
            checkBox.setSelected(true);
            checkBox.setOnAction(e -> onCheckboxSelected(itemID, checkBox));
            cartBox.getChildren().add(checkBox);
            cartRows.put(itemID, checkBox);
            cart.put(itemID, 1);
            cartPrices.put(itemID, price);
        } else cart.merge(itemID, 1, Integer::sum);
        totalPrice = totalPrice.add(price);
        int quantity = cart.get(itemID);
        BigDecimal lineTotal = cartPrices.get(itemID).multiply(BigDecimal.valueOf(quantity));
        cartRows.get(itemID).setText(quantity + "x" + name + " $%.2f".formatted(lineTotal));
        totalPriceLabel.setText("Total Price: $%.2f".formatted(totalPrice));
    }

    private void onCheckboxSelected(int itemID, CheckBox parent) {
        if (!parent.isSelected()) {
            totalPrice = totalPrice.subtract(cartPrices.get(itemID).multiply(BigDecimal.valueOf(cart.get(itemID))));
            cartBox.getChildren().remove(parent);
            cart.remove(itemID);
            cartPrices.remove(itemID);
            cartRows.remove(itemID);
            totalPriceLabel.setText("Total Price: $%.2f".formatted(totalPrice));
        }
    }

    private void closeWindow() {
        controllerActive = false;
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    private void confirmCheckout() {
        if (cart.isEmpty() || checkoutInProgress || checkoutOutcomeUncertain) return;
        checkoutInProgress = true;
        checkout.setDisable(true);
        mainBox.setDisable(true);
        sideBox.setDisable(true);
        drinkBox.setDisable(true);
        cartBox.setDisable(true);
        TreeMap<Integer, Integer> checkoutCart = new TreeMap<>(cart);
        TreeMap<Integer, BigDecimal> checkoutPrices = new TreeMap<>(cartPrices);
        Task<CheckoutOutcome> task = new Task<>() {
            @Override
            protected CheckoutOutcome call() {
                return executeCheckout(checkoutCart, checkoutPrices);
            }
        };
        task.setOnSucceeded(event -> {
            CheckoutOutcome outcome = task.getValue();
            if (outcome == CheckoutOutcome.SUCCESS && controllerActive) {
                for (CheckBox item : cartRows.values()) cartBox.getChildren().remove(item);
                cartRows.clear();
                cart.clear();
                cartPrices.clear();
                totalPrice = BigDecimal.ZERO;
                totalPriceLabel.setText("Total Price: 0.0$");
                showInformation("Order Complete", "Order recorded successfully.");
            } else if (outcome == CheckoutOutcome.UNCERTAIN && controllerActive) {
                checkoutOutcomeUncertain = true;
                showError("Checkout Status Unknown", "The order status could not be confirmed.",
                        "Do not retry. Verify the receipt with a manager before submitting another order.");
            } else if (outcome == CheckoutOutcome.FAILURE && controllerActive) {
                showError("Checkout Failed", "The order was not recorded.",
                        "Please verify inventory and try again.");
            }
            if (outcome != CheckoutOutcome.UNCERTAIN) finishCheckout();
        });
        task.setOnFailed(event -> {
            if (controllerActive) showError("Checkout Failed", "The order was not recorded.",
                    "Please verify inventory and try again.");
            finishCheckout();
        });
        task.setOnCancelled(event -> finishCheckout());
        if (!startTask(task)) {
            finishCheckout();
            showError("Checkout Failed", "The database is busy.", "Please try again.");
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

    private Connection openConnection(String user, String password) throws SQLException, ClassNotFoundException {
        Class.forName("org.postgresql.Driver");
        DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SECONDS);
        Connection conn = DriverManager.getConnection(DB_URL, user, password);
        conn.setNetworkTimeout(Runnable::run, NETWORK_TIMEOUT_MILLISECONDS);
        return conn;
    }

    private enum CheckoutOutcome {
        SUCCESS, FAILURE, UNCERTAIN
    }

    private CheckoutOutcome executeCheckout(TreeMap<Integer, Integer> checkoutCart,
            TreeMap<Integer, BigDecimal> checkoutPrices) {
        Connection conn = null;
        try {
            dbSetup my = new dbSetup();
            conn = openConnection(my.user, my.pswd);
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement receipt = conn.prepareStatement(
                        "INSERT INTO receipt (location_id, user_id, receipt_timestamp) VALUES (?, ?, CURRENT_TIMESTAMP) RETURNING receipt_id");
                        PreparedStatement itemValidation = conn.prepareStatement(
                                "SELECT i.active, COUNT(il.inventory_id) AS ingredient_count "
                                        + "FROM item i LEFT JOIN ingredient_list il ON il.item_id = i.item_id "
                                        + "WHERE i.item_id = ? GROUP BY i.active");
                        PreparedStatement ingredients = conn.prepareStatement(
                                "SELECT inventory_id FROM ingredient_list WHERE item_id = ? ORDER BY inventory_id FOR SHARE");
                        PreparedStatement update = conn.prepareStatement(
                                "UPDATE inventory SET stock = stock - ? WHERE inventory_id = ? AND stock >= ?");
                        PreparedStatement receiptItem = conn.prepareStatement(
                                "INSERT INTO receipt_item (receipt_id, item_id, quantity, price_at_sale) "
                                        + "SELECT ?, item_id, ?, ? FROM item "
                                        + "WHERE item_id = ? AND active = TRUE AND price = ?")) {
                    for (Integer itemID : checkoutCart.keySet()) {
                        itemValidation.setInt(1, itemID);
                        try (ResultSet validationResults = itemValidation.executeQuery()) {
                            if (!validationResults.next()
                                    || !validationResults.getBoolean("active")
                                    || validationResults.getInt("ingredient_count") == 0) {
                                throw new SQLException("Item is unavailable or has no ingredient mapping: " + itemID);
                            }
                        }
                        if (!checkoutPrices.containsKey(itemID))
                            throw new SQLException("Price snapshot is missing for item " + itemID);
                    }
                    receipt.setInt(1, currentLocationId);
                    receipt.setInt(2, currentUserId);
                    int receiptID;
                    try (ResultSet receiptResults = receipt.executeQuery()) {
                        if (!receiptResults.next()) throw new SQLException("Unable to create receipt");
                        receiptID = receiptResults.getInt("receipt_id");
                    }
                    TreeMap<Integer, Integer> requiredInventory = new TreeMap<>();
                    for (Map.Entry<Integer, Integer> entry : checkoutCart.entrySet()) {
                        int itemID = entry.getKey();
                        int expectedIngredientCount;
                        itemValidation.setInt(1, itemID);
                        try (ResultSet validationResults = itemValidation.executeQuery()) {
                            if (!validationResults.next()
                                    || !validationResults.getBoolean("active")
                                    || validationResults.getInt("ingredient_count") == 0) {
                                throw new SQLException("Item is no longer available: " + itemID);
                            }
                            expectedIngredientCount = validationResults.getInt("ingredient_count");
                        }
                        int mappedIngredientCount = 0;
                        ingredients.setInt(1, itemID);
                        try (ResultSet ingredientResults = ingredients.executeQuery()) {
                            while (ingredientResults.next()) {
                                int inventoryID = ingredientResults.getInt("inventory_id");
                                requiredInventory.merge(inventoryID, entry.getValue(), Integer::sum);
                                mappedIngredientCount++;
                            }
                        }
                        if (mappedIngredientCount != expectedIngredientCount)
                            throw new SQLException("Ingredient mapping changed for item " + itemID);
                    }
                    for (Map.Entry<Integer, Integer> entry : requiredInventory.entrySet()) {
                        update.setInt(1, entry.getValue());
                        update.setInt(2, entry.getKey());
                        update.setInt(3, entry.getValue());
                        if (update.executeUpdate() == 0)
                            throw new SQLException("Insufficient inventory for checkout");
                    }
                    for (Map.Entry<Integer, Integer> entry : checkoutCart.entrySet()) {
                        int itemID = entry.getKey();
                        BigDecimal lineTotal = checkoutPrices.get(itemID)
                                .multiply(BigDecimal.valueOf(entry.getValue()));
                        if (lineTotal.scale() > 2 || lineTotal.signum() < 0
                                || lineTotal.compareTo(new BigDecimal("99999999.99")) > 0)
                            throw new SQLException("Checkout total is outside the database price range");
                        receiptItem.setInt(1, receiptID);
                        receiptItem.setInt(2, itemID);
                        receiptItem.setBigDecimal(3, lineTotal);
                        receiptItem.setInt(4, itemID);
                        receiptItem.setBigDecimal(5, checkoutPrices.get(itemID));
                        if (receiptItem.executeUpdate() == 0)
                            throw new SQLException("Item price or availability changed: " + itemID);
                    }
                }
            } catch (Exception e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackError) {
                    return CheckoutOutcome.UNCERTAIN;
                }
                return CheckoutOutcome.FAILURE;
            }
            try {
                conn.commit();
                return CheckoutOutcome.SUCCESS;
            } catch (SQLException commitError) {
                return CheckoutOutcome.UNCERTAIN;
            }
        } catch (SQLException | ClassNotFoundException e) {
            return CheckoutOutcome.FAILURE;
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException ignored) { }
        }
    }

    private void finishCheckout() {
        if (!controllerActive) return;
        checkoutInProgress = false;
        checkout.setDisable(false);
        mainBox.setDisable(false);
        sideBox.setDisable(false);
        drinkBox.setDisable(false);
        cartBox.setDisable(false);
    }

    private void showInformation(String title, String details) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(details);
        alert.showAndWait();
    }

    private void showError(String title, String header, String details) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(details);
        alert.showAndWait();
    }

    @FXML public void changeView(ActionEvent event) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/managerGUI.fxml"));
        controllerActive = false;
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.setTitle("Manager View");
        stage.setScene(new Scene(root, 1200, 800));
        stage.show();
    }
}
