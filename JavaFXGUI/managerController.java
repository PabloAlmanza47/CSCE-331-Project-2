import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.sql.*;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.BarChart;
import javafx.scene.control.ListView;
import javafx.collections.ObservableList;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Label;
import javafx.scene.control.Accordion;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;


public class managerController{
    private static final String DB_URL = "jdbc:postgresql://csce-315-db.engr.tamu.edu/team1db"; //database location
    
    // This method runs automatically when the FXML loads
    @FXML
    public void initialize() {
        /*// Set up what happens when button is clicked
        queryButton.setOnAction(event -> runQuery());
        closeButton.setOnAction(event -> closeWindow());*/
        runQuery();
    }
    
    // Your method to run the database query
    private void runQuery() {
        try {
        // Get database creditials
        dbSetup my = new dbSetup();
    
        // Build the connection
        Class.forName("org.postgresql.Driver");
        Connection conn = DriverManager.getConnection(DB_URL, my.user, my.pswd);

        // Create statement
        Statement stmt = conn.createStatement();

        // Run sql query
        //Bar Graph query and call
        ResultSet items = stmt.executeQuery("SELECT i.name AS itemNames, SUM(ri.quantity) AS numSold FROM receipt_item ri JOIN item i ON i.item_id = ri.item_id GROUP BY i.name ORDER BY numSold");
        createSalesBarGraph(items);

        //Stock query and call
        ResultSet stocklist = stmt.executeQuery("SELECT inventory_id, name, stock, min_stock, next_shipment, shelf_life FROM inventory ORDER BY inventory_id");
        createStockList(stocklist);

        //Receipt list query and call
        ResultSet receipts = stmt.executeQuery("SELECT receipt_item_id, receipt_id, item_id, quantity, price_at_sale FROM receipt_item");
        createReceiptList(receipts);

        // Close connection
        stmt.close();
        conn.close();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
     * Closes the current window when the close button is clicked.
     * @author Ashley Hoang
     * @param event - the action event triggered by clicking the close button
    */
    @FXML public void closeWindow(ActionEvent event) {
        Button closeButton = (Button) event.getSource();
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }


    //Sales Graph creation
    @FXML BarChart<Number, String> salesGraph;

    /**
     * Creates a bar graph of the number of items sold from the provided ResultSet.
     * @author Ashley Hoang
     * @param items - the result set from the database query containing item names and number sold
     * @throws SQLException - if there is an error with the database query
     * @throws IllegalArgumentException - if there is an error with the list creation
    */
    @FXML public void createSalesBarGraph(ResultSet items){
        try {
            //add items
            XYChart.Series<Number, String> soldSeries = new XYChart.Series<>(); 
            while(items.next()){
                String name = items.getString("itemNames");
                int sold = items.getInt("numSold");

                soldSeries.getData().add(new XYChart.Data<>(sold, name));

            }
            salesGraph.getData().add(soldSeries);

        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    //Stock list creation
    @FXML public ListView<String> stockListView;

    /**
     * Creates a list of stock information from the provided ResultSet.
     * @author Ashley Hoang
     * @param stockList - the result set from the database query containing stock information
     * @throws SQLException - if there is an error with the database query
     * @throws IllegalArgumentException - if there is an error with the list creation
    */
    @FXML public void createStockList(ResultSet stockList){
        try {
            ObservableList<String> names = FXCollections.observableArrayList();
            while(stockList.next()){
                int inventoryID = stockList.getInt("inventory_id");
                String itemName = stockList.getString("name");
                int itemStock = stockList.getInt("stock");
                int minStock = stockList.getInt("min_stock");
                String nextShipment = stockList.getString("next_shipment");
                String shelfLife = stockList.getString("shelf_life");

                String display = String.format(
                    "#%d %s:\n- Stock: %d\n- Minimum Stock: %d\n- Next Shipment: %s\n- Shelf Life: %s",
                    inventoryID, itemName, itemStock, minStock, nextShipment, shelfLife
                );
                names.add(display);
            }
            stockListView.setItems(names);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    //Receipt list creation
    @FXML Accordion receiptAccordion;
    /**
     * Creates a list of receipts from the provided ResultSet.
     * @author Ashley Hoang
     * @param receipts - the result set from the database query containing receipt information
     * @throws SQLException - if there is an error with the database query
     * @throws IllegalArgumentException - if there is an error with the list creation
    */
    @FXML public void createReceiptList(ResultSet receipts){
        try {
            int counter = 0;
            while(receipts.next()){
                //TODO: only ten receipts because it will break otherwise; probably should change it to last ten receipts
                if(counter >= 10) break;
                counter++;

                int receiptItemID = receipts.getInt("receipt_item_id");
                int receiptID = receipts.getInt("receipt_id");
                int itemID = receipts.getInt("item_id");
                int itemQuantity = receipts.getInt("quantity");
                int price = receipts.getInt("price_at_sale");

                //TODO: change the label to look like an actual receipt.
                Label label = new Label(String.format(
                    "#%d \n%s: %d - %s\t$%d",
                    receiptID, receiptItemID, itemID, itemQuantity, price
                ));

                //sizing to fit tab, and adding to the accordion
                label.setMaxWidth(Double.MAX_VALUE);
                TitledPane receiptPane = new TitledPane("Receipt " + receiptID, label);
                receiptPane.setMaxWidth(Double.MAX_VALUE);
                receiptAccordion.getPanes().add(receiptPane);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
     * Changes the view to the cashier view when the corresponding button is clicked.
     * @author Ashley Hoang
     * @param event - the action event triggering the view change
     * @throws IOException if the FXML resource cannot be loaded
    */
    @FXML public void changeView(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/cashierGUI.fxml"));

        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();

        stage.setScene(new Scene(root, 1200, 800));
        stage.show();
    }
}
