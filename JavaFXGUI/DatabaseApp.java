import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DatabaseApp extends Application {
  @Override
  public void start(Stage stage) throws Exception {
    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("database-view.fxml"));
    Scene scene = new Scene(fxmlLoader.load(), 600, 400);
    stage.setTitle("AWS PostgreSQL Query Example");
    stage.setScene(scene);
    stage.show();
  }

  public static void main(String[] args) {
    launch();
  }
}
