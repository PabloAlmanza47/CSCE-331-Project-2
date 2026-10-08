import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.BarChart;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

public class managerController {
    private static final String DB_URL = "jdbc:postgresql://csce-315-db.engr.tamu.edu/team1db";

    @FXML public void initialize() { runQuery(); }

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
                        "SELECT item_id, name, price FROM item ORDER BY item_id")) {
                    createMenuList(menuList);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML public void closeWindow(ActionEvent event) {
        Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
        stage.close();
    }

    @FXML BarChart<Number, String> salesGraph;

    @FXML public void createSalesBarGraph(ResultSet items) {
        try {
            XYChart.Series<Number, String> soldSeries = new XYChart.Series<>();
            while (items.next()) {
                soldSeries.getData().add(new XYChart.Data<>(items.getInt("numSold"), items.getString("itemNames")));
            }
            salesGraph.getData().add(soldSeries);
        } catch (SQLException | IllegalArgumentException e) { e.printStackTrace(); }
    }

    // Preserve remote stock editing through StockItem and StockListCell.
    @FXML public ListView<StockItem> stockListView;

    @FXML public void createStockList(ResultSet stockList) {
        try {
            ObservableList<StockItem> stockItems = FXCollections.observableArrayList();
            while (stockList.next()) {
                stockItems.add(new StockItem(stockList.getInt("inventory_id"), stockList.getString("name"),
                        stockList.getInt("stock"), stockList.getInt("min_stock"),
                        stockList.getString("next_shipment"), stockList.getString("shelf_life")));
            }
            stockListView.setItems(stockItems);
            stockListView.setCellFactory(lv -> new StockListCell(this));
        } catch (SQLException | IllegalArgumentException e) { e.printStackTrace(); }
    }

    public void updateStockInDatabase(int inventoryID, int newStock) {
        try {
            dbSetup my = new dbSetup();
            Class.forName("org.postgresql.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, my.user, my.pswd);
                    PreparedStatement pstmt = conn.prepareStatement(
                            "UPDATE inventory SET stock = ? WHERE inventory_id = ?")) {
                pstmt.setInt(1, newStock);
                pstmt.setInt(2, inventoryID);
                pstmt.executeUpdate();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML Accordion receiptAccordion;

    @FXML public void createReceiptList(ResultSet receipts) {
        try {
            int currentReceiptID = -1;
            VBox itemList = null;
            BigDecimal receiptTotal = BigDecimal.ZERO;
            TitledPane receiptPane = null;
            while (receipts.next()) {
                int receiptID = receipts.getInt("receipt_id");
                if (receiptID != currentReceiptID) {
                    if (receiptPane != null) itemList.getChildren().add(new Label(
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
                if (lineTotal == null) lineTotal = BigDecimal.ZERO;
                receiptTotal = receiptTotal.add(lineTotal);
                itemList.getChildren().add(new Label(String.format(
                        "%s    x%d    $%.2f", itemName, quantity, lineTotal)));
            }
            if (receiptPane != null) itemList.getChildren().add(new Label(
                    String.format("Total: $%.2f", receiptTotal)));
        } catch (SQLException | IllegalArgumentException e) { e.printStackTrace(); }
    }

    @FXML public ListView<String> menuListView;

    @FXML public void createMenuList(ResultSet menuList) {
        try {
            ObservableList<String> names = FXCollections.observableArrayList();
            while (menuList.next()) {
                names.add(String.format("#%d %s:\n- Price: $%.2f", menuList.getInt("item_id"),
                        menuList.getString("name"), menuList.getBigDecimal("price")));
            }
            menuListView.setItems(names);
        } catch (SQLException | IllegalArgumentException e) { e.printStackTrace(); }
    }

    @FXML public void changeView(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/cashierGUI.fxml"));
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1200, 800));
        stage.show();
    }
}
