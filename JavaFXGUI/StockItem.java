import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

// This class represents a stock item with its properties and provides getters and setters for each property.
public class StockItem {
    private final IntegerProperty inventoryID = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final IntegerProperty stock = new SimpleIntegerProperty();
    private final IntegerProperty minStock = new SimpleIntegerProperty();
    private final StringProperty nextShipment = new SimpleStringProperty();
    private final StringProperty shelfLife = new SimpleStringProperty();

    public StockItem(int inventoryID, String name, int stock, int minStock, String nextShipment, String shelfLife) {
        this.inventoryID.set(inventoryID);
        this.name.set(name);
        this.stock.set(stock);
        this.minStock.set(minStock);
        this.nextShipment.set(nextShipment);
        this.shelfLife.set(shelfLife);
    }

    // Getters and setters for each field
    public int getInventoryID() { return inventoryID.get(); }
    public String getName() { return name.get(); }
    public int getStock() { return stock.get(); }
    public int getMinStock() { return minStock.get(); }
    public String getNextShipment() { return nextShipment.get(); }
    public String getShelfLife() { return shelfLife.get(); }

    public void setStock(int value) { stock.set(value); }
    public IntegerProperty stockProperty() { return stock; }
}