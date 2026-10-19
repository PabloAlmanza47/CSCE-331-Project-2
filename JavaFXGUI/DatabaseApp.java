import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Starts the JavaFX cashier application.
 */
public class DatabaseApp extends Application {
  /**
   * Creates the application instance.
   */
  public DatabaseApp() {
  }

  /**
   * Loads and displays the cashier view.
   *
   * @param stage the primary stage supplied by JavaFX
   * @throws Exception if the cashier view cannot be loaded
   */
  @Override
  public void start(Stage stage) throws Exception {
    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("cashierGUI.fxml"));
    Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
    stage.setTitle("Cashier View");
    stage.setScene(scene);
    stage.show();
  }

   /**
    * Launches the JavaFX application.
    *
    * @param args command-line arguments passed to the application
    */
   public static void main(String[] args) {
      launch();
   }
}
