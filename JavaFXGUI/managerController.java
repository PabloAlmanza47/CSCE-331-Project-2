import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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



//import manager;

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
        ResultSet stocklist = stmt.executeQuery("SELECT inventory_id, name, stock, min_stock, next_shipment, shelf_life FROM inventory");
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

    /*private void closeWindow() { 
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }*/

    //Sales Graph creation
    @FXML BarChart<Number, String> salesGraph;
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

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    //Stock list creation
    @FXML public ListView<String> stockListView;
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
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    //Receipt list creation
    @FXML Accordion receiptAccordion;
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
                TitledPane receiptPane = new TitledPane("Receipt " + receiptID, label);
                receiptAccordion.getPanes().add(receiptPane);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    //change view to cashier
    @FXML public void changeView(ActionEvent event) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/database-view.fxml"));

        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();

        stage.setScene(new Scene(root, 600, 400));
        stage.show();
    }
}
