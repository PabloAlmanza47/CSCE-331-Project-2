import java.awt.Checkbox;
import java.beans.EventHandler;
import java.sql.*;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javax.print.attribute.standard.Sides;

public class DatabaseController {
    
  @FXML
  private Button queryButton; //match the fx:id value from Scene Builder
  
  @FXML
  private VBox mainBox, sideBox, drinkBox, cartBox; //match the fx:id value from Scene Builder
  
  @FXML
  private Button closeButton; //match the fx:id value from Scene Builder
  
  private static final String DB_URL = "jdbc:postgresql://csce-315-db.engr.tamu.edu/team1db"; //database location
  
  // This method runs automatically when the FXML loads
  @FXML
  public void initialize() {
    // Set up what happens when button is clicked
    // queryButton.setOnAction(event -> runQuery());
    runQuery();
    closeButton.setOnAction(event -> closeWindow());
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

      for (int i = 0; i < areas.length; i++) {
        VBox currentBox = areas[i];

        ResultSet resultSet = stmt.executeQuery("SELECT name, price FROM item WHERE category_id = " + (i + 1));

        while (resultSet.next()) {
          
          String name = resultSet.getString("name");
          String price = resultSet.getString("price");

          Checkbox checkBox = new CheckBox(name + " $" + price); // change to spinner 
          Label itemLabel = new Label(name + " $" + price);

          currentBox.getChildren().add(checkBox);

          checkBox.setOnAction(event -> {
            if (checkBox.isSelected()){
              cartBox.getChildren().add(cartItem); // when selected, adds to cart
            }
            else {
              cartBox.getChildren().remove(cartItem); 
            }
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
}
