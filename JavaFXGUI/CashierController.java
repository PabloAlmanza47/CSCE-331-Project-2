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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.event.ActionEvent;
import javafx.scene.text.*;

/**
 * Class controller for cashier view fxml
 * @author Sharaf Mehmood
 */
public class CashierController {
    @FXML private Button bowl, plate, biggerPlate, changeView, checkout, closeButton;
    @FXML private VBox mainBox, sideBox, drinkBox, cartBox;
    private Label MAIN_LABEL = new Label("Mains"), SIDE_LABEL = new Label("Sides"), DRINK_LABEL = new Label("Drinks"), totalPriceLabel;
    private static final String DB_URL = "jdbc:postgresql://csce-315-db.engr.tamu.edu/team1db";
    private String SELECTED_STYLE = "-fx-border-color: #D61927; -fx-font-weight: bold";
    private static final int LOGIN_TIMEOUT_SECONDS = 15;
    private static final int FONT_SIZE = 20;
    private static final int NETWORK_TIMEOUT_MILLISECONDS = 30000;
    private static final ExecutorService DATABASE_EXECUTOR = new ThreadPoolExecutor( //TODO: Put this in the constructor with a short explanation for why this is needed.
            4, 4, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(16),
            runnable -> {
                Thread thread = new Thread(runnable, "cashier-database-worker");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());
    private final List<CartLine> cartLines = new ArrayList<>();
    private MealSize selectedSize;
    private final List<MenuRecord> pendingMains = new ArrayList<>();
    private MenuRecord pendingSide;
    private BigDecimal totalPrice = BigDecimal.ZERO;
    private int currentLocationId = 1;
    private int currentUserId = 3;
    private boolean checkoutInProgress;
    private boolean checkoutOutcomeUncertain;
    private boolean controllerActive = true;

    /**
     * Load menu items and initialize button functionality onto the screen
     * @author Sharaf Mehmood
     */
    @FXML 
    public void initialize() {
        runQuery();
        closeButton.setOnAction(event -> closeWindow());
        bowl.setOnAction(event -> selectSize(MealSize.BOWL));
        plate.setOnAction(event -> selectSize(MealSize.PLATE));
        biggerPlate.setOnAction(event -> selectSize(MealSize.BIGGER_PLATE));
        checkout.setOnAction(event -> confirmCheckout());
    }

    /**
     * Attempts to connect to database and load menu items
     * @author // TODO
     */
    //TODO: Please refactor this, the function is very unclear on what it does and very ugly.
    private void runQuery() {
        checkout.setDisable(true);
        Task<List<MenuRecord>> task = new Task<>() {
            @Override
            protected List<MenuRecord> call() throws Exception {
                List<MenuRecord> menu = new ArrayList<>();
                Class.forName("org.postgresql.Driver");
                try (Connection conn = openConnection(DbSetup.user, DbSetup.pswd);
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
        
        task.setOnSucceeded(event -> loadBoxes(task));
        task.setOnFailed(event -> loadBoxesFail());

        if (!startTask(task)) {
            checkout.setDisable(false);
            showError("Menu Load Failed", "The database is busy.", "Please try again.");
        }
    }

    /**
     * Loads all of the menu items onto the screen, if task succeeds in connecting to database 
     * @param task represents the task of loading elements from the database
     * @author Pablo Almanza, Sharaf Mehmood
     */
    private void loadBoxes(Task<List<MenuRecord>> task){
        if (!controllerActive) return;
        VBox[] areas = { mainBox, sideBox, drinkBox };
        for (VBox area : areas) area.getChildren().clear();
        mainBox.getChildren().add(MAIN_LABEL);
        sideBox.getChildren().add(SIDE_LABEL);
        drinkBox.getChildren().add(DRINK_LABEL);
        if (totalPriceLabel == null) {
            totalPriceLabel = new Label("Total Price: 0.0$");
            cartBox.getChildren().add(totalPriceLabel);
            totalPriceLabel.getStyleClass().add("font");
        }
        for (MenuRecord record : task.getValue()) {
            VBox area = areas[record.getCategoryID() - 1];
            Button button = new Button(record.getName());
            button.setPrefSize(area.getPrefWidth(), 30);
            button.setOnAction(action -> onMenuItemPressed(record, button));
            area.getChildren().add(button);
        }
        checkout.setDisable(false);
    }

    /**
     * Shows an error if loading the areas does not work.
     * @author Pablo Almanza
     */
    private void loadBoxesFail(){
        if (!controllerActive) return;
        checkout.setDisable(false);
        showError("Menu Load Failed", "The menu could not be loaded.",
                "Please check the database connection and try again.");
    }

    /**
     * Clears any highlights that may have occured when selecting items.
     * @author Sharaf Mehmood
     */
    private void clearHighlights(){
        VBox[] areas = { mainBox, sideBox}; 
        for (VBox box : areas) {
            for (javafx.scene.Node node : box.getChildren()) node.setStyle("");
        }
    }

    /**
     * Handles size selection (bowl, plate, bigger plate)
     * @author Sharaf Mehmood
     */
    private void selectSize(MealSize size){
        if (selectedSize == size) selectedSize = null;
        else selectedSize = size;
        pendingMains.clear();
        pendingSide = null;
        clearHighlights();

        if (selectedSize == MealSize.BOWL) {
            bowl.setStyle(SELECTED_STYLE);
        }
        else bowl.setStyle("");

        if (selectedSize == MealSize.PLATE) {
            plate.setStyle(SELECTED_STYLE);
        }
        else plate.setStyle("");
    
        if (selectedSize == MealSize.BIGGER_PLATE) { 
            biggerPlate.setStyle(SELECTED_STYLE);
        }
        else biggerPlate.setStyle("");
    }

    /**
     * Handles selecting menu items, checking whether a meal is being formed or if orders are a la carte.
     * @param record MenuRecord tracking all components of the menu item clicked
     * @param button the corresponding button for the menu item clicked
     * @author Sharaf Mehmood
     */
    private void onMenuItemPressed(MenuRecord record, Button button) { 
        if (selectedSize == null || record.getCategoryID() == 3) {
            addToCart(CartLine.single(record));
            return;
        }
        if (record.getCategoryID() == 1 && pendingMains.size() < selectedSize.getMains()) { 
            pendingMains.add(record);
            button.setStyle(SELECTED_STYLE);
        }
        if (record.getCategoryID() == 2) { 
            pendingSide = record;
            button.setStyle(SELECTED_STYLE);
        } 
        if (pendingMains.size() == selectedSize.getMains() && pendingSide != null) {
            addToCart(CartLine.meal(selectedSize, pendingMains, pendingSide));
            pendingMains.clear();
            pendingSide = null;
            clearHighlights();
            selectSize(selectedSize);
        }
    }

    /**
     * Handles selecting menu items, checking whether a meal is being formed or if orders are a la carte.
     * @param line represents the item in the cart being changed
     * @param change the amount the item's quantity is being changed by
     * @author Sharaf Mehmood
     */
    private void changeQuantity(CartLine line, int change) {
        int newQuantity = line.getQuantity() + change;
        if (newQuantity < 1) {
            cartBox.getChildren().remove(line.getRow().getParent());
            cartLines.remove(line);
        } else {
            line.setQuantity(newQuantity);
            line.getRow().setText(line.describe());
        }
        refreshTotal();
    }

    /**
     * Adds an item to cart (either as a meal or individually, based of if a size was selected)
     * @param line the cart line being added to the cart
     * @author Sharaf Mehmood
     */
    private void addToCart(CartLine line) {
        for (CartLine existing : cartLines) {
            if (line.getKey() != null && line.getKey().equals(existing.getKey())) {
                existing.setQuantity(existing.getQuantity() + 1);
                existing.getRow().setText(existing.describe());
                refreshTotal();
                return;
            }
        }
        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(true);
        checkBox.setWrapText(true);
        checkBox.setOnAction(e -> onCheckboxSelected(line, checkBox));
        checkBox.setText(line.describe());
        Button minus = new Button("-");                   
        Button plus = new Button("+");                     
        minus.setOnAction(e -> changeQuantity(line, -1));  
        plus.setOnAction(e -> changeQuantity(line, 1));    
        HBox.setHgrow(checkBox, Priority.ALWAYS);          
        checkBox.setMaxWidth(Double.MAX_VALUE);           
        line.setRow(checkBox);
        cartLines.add(line);
        cartBox.getChildren().add(new HBox(5, checkBox, minus, plus));
        refreshTotal();
    }

    /**
     * Refreshes total price label
     * @author Sharaf Mehmood
     */
    private void refreshTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (CartLine line : cartLines) total = total.add(line.lineTotal());
        totalPriceLabel.setText("Total Price: $%.2f".formatted(total));
    }

    /**
     * Removes the corresponding item from cart when selected (unchecked)
     * @param line represents the item in the cart
     * @param parent represents the checkbox being unchecked
     * @author Sharaf Mehmood
     */
    private void onCheckboxSelected(CartLine line, CheckBox parent) {
        if (!parent.isSelected()) {
            cartBox.getChildren().remove(parent.getParent());
            cartLines.remove(line);
            refreshTotal();
        }
    }

    /**
     * Closes the current window
     * @author Sharaf Mehmood
     */
    private void closeWindow() {
        controllerActive = false;
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Represents what happens on a success with checkout
     * @param task 
     * @author Pablo Almanza
     */
    private void checkoutSuccess(Task<CheckoutOutcome> task){
        CheckoutOutcome outcome = task.getValue();
        if (outcome == CheckoutOutcome.SUCCESS && controllerActive) {
            for (CartLine line : cartLines) cartBox.getChildren().remove(line.getRow().getParent());
            cartLines.clear();
            refreshTotal();
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
    }

    /**
     * Represents what happens on a failure to checkout
     * @author Pablo Almanza
     */
    private void checkoutFail(){
        if (controllerActive) showError("Checkout Failed", "The order was not recorded.",
                    "Please verify inventory and try again.");
        finishCheckout();
    }

    /**
     * Converts cart line objects into maps to execute checkout
     * @param quantities Tree Map of item ID and their quantity
     * @param listPrices Tree Map of item ID and their normal menu price
     * @param totals Tree Map of item ID and the total amount to record
     * @author Sharaf Mehmood
     */
    private void flattenCart(TreeMap<Integer, Integer> quantities, TreeMap<Integer, BigDecimal> listPrices,
            TreeMap<Integer, BigDecimal> totals) {
        for (CartLine line : cartLines) {
            BigDecimal quantity = BigDecimal.valueOf(line.getQuantity());
            for (int i = 0; i < line.getComponents().size(); i++) {
                MenuRecord component = line.getComponents().get(i);
                BigDecimal unitAmount = (i == 0) ? component.getPrice().add(line.discount()) : component.getPrice();
                quantities.merge(component.getItemID(), line.getQuantity(), Integer::sum);
                listPrices.put(component.getItemID(), component.getPrice());
                totals.merge(component.getItemID(), unitAmount.multiply(quantity), BigDecimal::add);
            }
        }
    }

    /**
     * DESCRIPTION // TODO
     * @author // TODO
     */
    private void confirmCheckout() {
        if (cartLines.isEmpty() || checkoutInProgress || checkoutOutcomeUncertain) return;
        checkoutInProgress = true;
        checkout.setDisable(true);
        mainBox.setDisable(true);
        sideBox.setDisable(true);
        drinkBox.setDisable(true);
        cartBox.setDisable(true);
        TreeMap<Integer, Integer> checkoutCart = new TreeMap<>();
        TreeMap<Integer, BigDecimal> checkoutPrices = new TreeMap<>();
        TreeMap<Integer, BigDecimal> checkoutTotal = new TreeMap<>();
        flattenCart(checkoutCart, checkoutPrices, checkoutTotal);
        Task<CheckoutOutcome> task = new Task<>() {
            @Override
            protected CheckoutOutcome call() {
                return executeCheckout(checkoutCart, checkoutPrices, checkoutTotal);
            }
        };

        task.setOnSucceeded(event -> checkoutSuccess(task));
        task.setOnFailed(event -> checkoutFail());
        task.setOnCancelled(event -> finishCheckout());

        if (!startTask(task)) {
            finishCheckout();
            showError("Checkout Failed", "The database is busy.", "Please try again.");
        }
    }

    /**
     * DESCRIPTION // TODO
     * @param task // TODO
     * @return // TODO
     * @author // TODO
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
     * DESCRIPTION // TODO
     * @param user // TODO
     * @param password // TODO
     * @return // TODO
     * @throws SQLException // TODO
     * @throws ClassNotFoundException // TODO
     * @author  // TODO
     */
    private Connection openConnection(String user, String password) throws SQLException, ClassNotFoundException {
        Class.forName("org.postgresql.Driver");
        DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SECONDS);
        Connection conn = DriverManager.getConnection(DB_URL, user, password);
        conn.setNetworkTimeout(Runnable::run, NETWORK_TIMEOUT_MILLISECONDS);
        return conn;
    }

    /**
     * DESCRIPTION // TODO
     * @author // TODO
     */
    private enum CheckoutOutcome {
        SUCCESS, FAILURE, UNCERTAIN
    }

    /**
     * DESCRIPTION // Updates inventory, receipt, and front end data once checkout button is clicked
     * @param checkoutCart Tree map of item ID's and their quantity 
     * @param checkoutPrices Tree map of item ID's and their prices
     * @param checkoutTOtals Tree map of item ID's and their total price
     * @return Only returns errors
     * @author Colby Ideker, Pablo Almanza, Sharaf Mehmood
     * Note - Please add helper functions for this, the function is very long and hard to read.
     */
    private CheckoutOutcome executeCheckout(TreeMap<Integer, Integer> checkoutCart,
            TreeMap<Integer, BigDecimal> checkoutPrices, TreeMap<Integer, BigDecimal> checkoutTotals) {
        Connection conn = null;
        try {
            conn = openConnection(DbSetup.user, DbSetup.pswd);
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
                        BigDecimal lineTotal = checkoutTotals.get(itemID);
                        if (lineTotal.scale() > 2 || lineTotal.signum() < 0
                                || lineTotal.compareTo(new BigDecimal("99999999.99")) > 0)
                            throw new SQLException("Checkout total is outside the database price range");
                        receiptItem.setInt(1, receiptID);
                        receiptItem.setInt(2, entry.getValue());
                        receiptItem.setBigDecimal(3, lineTotal);
                        receiptItem.setInt(4, itemID);
                        receiptItem.setBigDecimal(5, checkoutPrices.get(itemID));
                        if (receiptItem.executeUpdate() == 0)
                            throw new SQLException("Item price or availability changed: " + itemID);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
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
            e.printStackTrace();
            return CheckoutOutcome.FAILURE;
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException ignored) { }
        }
    }

    /**
     * DESCRIPTION // TODO
     * @author // TODO
     */
    private void finishCheckout() {
        if (!controllerActive) return;
        checkoutInProgress = false;
        checkout.setDisable(false);
        mainBox.setDisable(false);
        sideBox.setDisable(false);
        drinkBox.setDisable(false);
        cartBox.setDisable(false);
    }

    /**
     * DESCRIPTION // TODO
     * @param title // TODO
     * @param details // TODO
     * @author // TODO
     */
    private void showInformation(String title, String details) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(details);
        alert.showAndWait();
    }

    /**
     * DESCRIPTION // TODO
     * @param title // TODO
     * @param header // TODO
     * @param details // TODO
     * @author // TODO
     */
    private void showError(String title, String header, String details) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(details);
        alert.showAndWait();
    }

    /**
     * Changes view from cashier to manager
     * @param event event of clicking change view button
     * @throws Exception 
     * @author Sharaf Mehmood
     */
    @FXML public void changeView(ActionEvent event) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/managerGUI.fxml"));
        controllerActive = false;
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.setTitle("Manager View");
        stage.setScene(new Scene(root, 1200, 800));
        stage.show();
    }
}
