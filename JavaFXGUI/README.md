GUI Design
- Both cashier and manager GUI have buttons to switch view to the other.
- Cashier GUI allows the user to add/remove items to the shopping cart and checkout.
- Checking out the shopping cart updates the database.
- Manager GUI allows the user to view a bar graph of sales and most recent receipts.
- Manager GUI allows the user to increase/decrease the stock of inventory items which updates the database.
- Manager GUI allows the user to increase/decrease the price of menu items as well as add/remove menu items which also updates the database.
- From Phase 0, GUI designs have changed slightly which includes adding buttons and a new tab for manager. More details in CHANGELOG.md.

Use the same dbSetup.java code from jdbc_demo
Assumes javafx sdk is in parent directory
In the commands below, Windows uses ;, for Mac/Linux/WSL, use :

Compile:
> javac --module-path ../javafx-sdk-25/lib/ --add-modules javafx.controls,javafx.fxml -cp ".;postgresql-42.2.8.jar" DatabaseApp.java cashierController.java
> javac --module-path ~/javafx-sdk-25/lib/ --add-modules javafx.controls,javafx.fxml -cp ".:postgresql-42.2.8.jar" DatabaseApp.java cashierController.java

Run:
> java --enable-native-access=javafx.graphics --module-path ../javafx-sdk-25/lib/ --add-modules javafx.controls,javafx.fxml -cp ".;postgresql-42.2.8.jar" DatabaseApp.java
> java --enable-native-access=javafx.graphics --module-path ~/javafx-sdk-25/lib/ --add-modules javafx.controls,javafx.fxml -cp ".:postgresql-42.2.8.jar" DatabaseApp.java

Do Both:
> javac --module-path ~/javafx-sdk-25/lib/ --add-modules javafx.controls,javafx.fxml -cp ".:postgresql-42.2.8.jar" DatabaseApp.java cashierController.java && java --enable-native-access=javafx.graphics --module-path ~/javafx-sdk-25/lib/ --add-modules javafx.controls,javafx.fxml -cp ".:postgresql-42.2.8.jar" DatabaseApp.java

Doc (run in root project directory):
> javadoc -d doc --module-path ~/javafx-sdk-25/lib/ --add-modules javafx.controls,javafx.fxml -cp ".:postgresql-42.2.8.jar" -sourcepath ./ JavaFXGUI/*.java
