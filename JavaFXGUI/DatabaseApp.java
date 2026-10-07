import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DatabaseApp extends Application {
  @Override
  public void start(Stage stage) throws Exception {
    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("cashierGUI.fxml"));
    Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
    stage.setTitle("Cashier View");
    stage.setScene(scene);
    stage.show();
  }

   public static void main(String[] args) {
      launch();
   }
}
