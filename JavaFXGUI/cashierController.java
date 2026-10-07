import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javax.print.attribute.standard.Sides;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

public class cashierController {
    
  @FXML
  private Button changeView, checkout; //match the fx:id value from Scene Builder
  
  @FXML
  private VBox mainBox, sideBox, drinkBox, cartBox; //match the fx:id value from Scene Builder
  
  @FXML
  private Button closeButton; //match the fx:id value from Scene Builder
  
  private static final String DB_URL = "jdbc:postgresql://csce-315-db.engr.tamu.edu/team1db"; //database location

  private HashMap<Integer, Integer> cart = new HashMap<Integer, Integer>();
  private HashMap<Integer, CheckBox> cartRows = new HashMap<>();
  private double totalPrice;

  // This method runs automatically when the FXML loads
  @FXML
  public void initialize() {
    // Set up what happens when button is clicked
    // queryButton.setOnAction(event -> runQuery());
    runQuery();
    closeButton.setOnAction(event -> closeWindow());
    checkout.setOnAction(event -> confirmCheckout());
    //changeView.setOnAction(event -> changeView());
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

      VBox[] areas = { mainBox, sideBox, drinkBox };

      Label totalPriceLabel = new Label("Total Price: 0.0$");
      cartBox.getChildren().add(totalPriceLabel);

      for (int i = 0; i < areas.length; i++) {
        VBox currentBox = areas[i];

        ResultSet resultSet = stmt.executeQuery("SELECT name, price, item_id FROM item WHERE category_id = " + (i + 1));

        while (resultSet.next()) {
          
          String name = resultSet.getString("name");
          double price = resultSet.getDouble("price");
          int itemID = resultSet.getInt("item_id");

          Button button = new Button(name);
          button.setPrefSize(currentBox.getPrefWidth(), 30);

          //Label cartItem = new Label(name + " $" + price);

          currentBox.getChildren().add(button);

          button.setOnAction(event -> {
            if (!cart.containsKey(itemID)) {            
              CheckBox checkBox = new CheckBox();
              checkBox.setSelected(true);
              checkBox.setOnAction(e -> {
                if (!checkBox.isSelected()) {
                  totalPrice -= price * cart.get(itemID);
                  cartBox.getChildren().remove(checkBox);
                  cart.remove(itemID);               
                  cartRows.remove(itemID);
                  totalPriceLabel.setText("Total Price: " + totalPrice   + "$");
                }
              });
              cartBox.getChildren().add(checkBox);
              cartRows.put(itemID, checkBox);
              cart.put(itemID, 1);
            } 
            else {                                  
              cart.merge(itemID, 1, Integer::sum);
            }
            totalPrice += price;
            int quantity = cart.get(itemID);
            cartRows.get(itemID).setText(quantity + "x" + name + " $" + (price * quantity));
            totalPriceLabel.setText("Total Price: " + totalPrice   + "$");
          });
        }

      }

      // Close connection
      stmt.close();
      conn.close();

    } catch (Exception e) {
      e.printStackTrace();
      System.exit(0);
    }
  }

  private void closeWindow() { 
    Stage stage = (Stage) closeButton.getScene().getWindow();
    stage.close();
  }

  private void confirmCheckout() {
    //update inventory
    dbSetup my = new dbSetup();
    try {
      //create statements for updating db
      Connection conn = DriverManager.getConnection(DB_URL, my.user, my.pswd);
      Statement stmt = conn.createStatement();
      PreparedStatement ingredients = conn.prepareStatement("SELECT inventory_id FROM ingredient_list WHERE item_id = ?");
      PreparedStatement update = conn.prepareStatement("UPDATE inventory SET stock = stock - ? WHERE inventory_id = ?");

      //create new receipt with proper ID, and then grab it, 
      //currently assumes location of 1, and userid of 3, fix if needed
      ResultSet receiptResults = stmt.executeQuery(
      "INSERT INTO receipt (receipt_id, location_id, user_id, receipt_timestamp) " +
      "SELECT COALESCE(MAX(receipt_id), 0) + 1, 1, 3, CURRENT_TIMESTAMP " +
      "FROM receipt RETURNING receipt_id");
      receiptResults.next();
      int receiptID = receiptResults.getInt("receipt_id");
      receiptResults.close();
      
      //for each cart item, find ingredients, remove ordered amount
      for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
        int itemID = entry.getKey();
        int quantity = entry.getValue();
        ingredients.setInt(1, itemID);
        ResultSet ingredientResults = ingredients.executeQuery();
        while (ingredientResults.next()) {
          update.setInt(1, entry.getValue());
          update.setInt(2, ingredientResults.getInt("inventory_id"));
          update.executeUpdate();
        }
        ingredientResults.close();
        //adds item, quantity, sale price to receipt, requires grabbing last receiptitemid
        stmt.executeUpdate(
          "INSERT INTO receipt_item (receipt_item_id, receipt_id, item_id, quantity, price_at_sale) " +
          "SELECT (SELECT COALESCE(MAX(receipt_item_id), 0) + 1 FROM receipt_item), " +
          receiptID + ", item_id, " + quantity + ", price * " + quantity +
          " FROM item WHERE item_id = " + itemID
        );
      }
      ingredients.close();
      update.close();
      stmt.close();
      conn.close();
      //catch any SQL related errors
    } catch (SQLException e) {
      e.printStackTrace();
    }
    for (Map.Entry<Integer, CheckBox> entry : cartRows.entrySet()){
      CheckBox item = entry.getValue();
      cartBox.getChildren().remove(item);
    }
    cartRows.clear();
    cart.clear();
    totalPrice = 0;
    totalPricceLabel.setText("Total Price: 0.0$")
  }
  //clear cart
  //change view to manager
  //TODO: add onAction="#changeView" to the desired button in database-view.fxml
  @FXML public void changeView(ActionEvent event) throws Exception {
      Parent root = FXMLLoader.load(getClass().getResource("/managerGUI.fxml"));

      Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();

      stage.setScene(new Scene(root, 600, 400));
      stage.show();
  }
}