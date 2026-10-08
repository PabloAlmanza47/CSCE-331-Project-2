import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import javafx.fxml.FXML;
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
    private final HashMap<Integer, Integer> cart = new HashMap<>();
    private final HashMap<Integer, CheckBox> cartRows = new HashMap<>();
    private double totalPrice;
    private int currentLocationId = 1;
    private int currentUserId = 3;

    @FXML public void initialize() {
        runQuery();
        closeButton.setOnAction(event -> closeWindow());
        checkout.setOnAction(event -> confirmCheckout());
    }

    private void runQuery() {
        try {
            dbSetup my = new dbSetup();
            Class.forName("org.postgresql.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, my.user, my.pswd);
                    Statement stmt = conn.createStatement()) {
                VBox[] areas = { mainBox, sideBox, drinkBox };
                totalPriceLabel = new Label("Total Price: 0.0$");
                cartBox.getChildren().add(totalPriceLabel);
                for (int i = 0; i < areas.length; i++) {
                    VBox currentBox = areas[i];
                    try (ResultSet resultSet = stmt.executeQuery(
                            "SELECT name, price, item_id FROM item WHERE category_id = " + (i + 1)
                                    + " AND active = TRUE")) {
                        while (resultSet.next()) {
                            String name = resultSet.getString("name");
                            double price = resultSet.getDouble("price");
                            int itemID = resultSet.getInt("item_id");
                            Button button = new Button(name);
                            button.setPrefSize(currentBox.getPrefWidth(), 30);
                            currentBox.getChildren().add(button);
                            button.setOnAction(event -> onButtonPressed(itemID, price, name));
                        }
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void onButtonPressed(int itemID, double price, String name) {
        if (!cart.containsKey(itemID)) {
            CheckBox checkBox = new CheckBox();
            checkBox.setSelected(true);
            checkBox.setOnAction(e -> onCheckboxSelected(itemID, price, checkBox));
            cartBox.getChildren().add(checkBox);
            cartRows.put(itemID, checkBox);
            cart.put(itemID, 1);
        } else cart.merge(itemID, 1, Integer::sum);
        totalPrice += price;
        int quantity = cart.get(itemID);
        cartRows.get(itemID).setText(quantity + "x" + name + " $%.2f".formatted(price * quantity));
        totalPriceLabel.setText("Total Price: $%.2f".formatted(totalPrice));
    }

    private void onCheckboxSelected(int itemID, double price, CheckBox parent) {
        if (!parent.isSelected()) {
            totalPrice -= price * cart.get(itemID);
            cartBox.getChildren().remove(parent);
            cart.remove(itemID);
            cartRows.remove(itemID);
            totalPriceLabel.setText("Total Price: $%.2f".formatted(totalPrice));
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    private void confirmCheckout() {
        if (cart.isEmpty()) return;
        dbSetup my = new dbSetup();
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL, my.user, my.pswd);
            conn.setAutoCommit(false);
            try (PreparedStatement receipt = conn.prepareStatement(
                    "INSERT INTO receipt (location_id, user_id, receipt_timestamp) VALUES (?, ?, CURRENT_TIMESTAMP) RETURNING receipt_id");
                    PreparedStatement ingredients = conn.prepareStatement(
                            "SELECT inventory_id FROM ingredient_list WHERE item_id = ?");
                    PreparedStatement update = conn.prepareStatement(
                            "UPDATE inventory SET stock = stock - ? WHERE inventory_id = ? AND stock >= ?");
                    PreparedStatement receiptItem = conn.prepareStatement(
                            "INSERT INTO receipt_item (receipt_id, item_id, quantity, price_at_sale) "
                                    + "SELECT ?, item_id, ?, price * ? FROM item WHERE item_id = ?")) {
                receipt.setInt(1, currentLocationId);
                receipt.setInt(2, currentUserId);
                int receiptID;
                try (ResultSet receiptResults = receipt.executeQuery()) {
                    if (!receiptResults.next()) throw new SQLException("Unable to create receipt");
                    receiptID = receiptResults.getInt("receipt_id");
                }
                for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
                    int itemID = entry.getKey();
                    int quantity = entry.getValue();
                    ingredients.setInt(1, itemID);
                    try (ResultSet ingredientResults = ingredients.executeQuery()) {
                        while (ingredientResults.next()) {
                            update.setInt(1, quantity);
                            update.setInt(2, ingredientResults.getInt("inventory_id"));
                            update.setInt(3, quantity);
                            if (update.executeUpdate() == 0)
                                throw new SQLException("Insufficient inventory for item " + itemID);
                        }
                    }
                    receiptItem.setInt(1, receiptID);
                    receiptItem.setInt(2, itemID);
                    receiptItem.setInt(3, quantity);
                    receiptItem.setInt(4, itemID);
                    if (receiptItem.executeUpdate() == 0)
                        throw new SQLException("Unable to add item " + itemID + " to receipt");
                }
                conn.commit();
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Order Complete");
                alert.setHeaderText(null);
                alert.setContentText("Order recorded successfully.");
                alert.showAndWait();
            }
            for (CheckBox item : cartRows.values()) cartBox.getChildren().remove(item);
            cartRows.clear();
            cart.clear();
            totalPrice = 0;
            totalPriceLabel.setText("Total Price: 0.0$");
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException rollbackError) { rollbackError.printStackTrace(); }
            e.printStackTrace();
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException closeError) { closeError.printStackTrace(); }
        }
    }

    @FXML public void changeView(ActionEvent event) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/managerGUI.fxml"));
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.setTitle("Manager View");
        stage.setScene(new Scene(root, 1200, 800));
        stage.show();
    }
}
