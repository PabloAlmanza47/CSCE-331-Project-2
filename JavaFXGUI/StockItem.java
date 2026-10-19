import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;

/**
 * This class represents a stock item with its properties and provides getters and setters for each property.
 * @author Ashley Hoang
 */
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

    /**
     * Calculates and returns what color the stock item should display based on the relationship of stock and min stock
     * @return Color of warning for stock
     * @author Ashley Hoang
     */
    public Color getColor(){
        //lower than minStock --> red; between minStock and minStock*1.5 --> orange; else white
        Color warningColor = Color.rgb(255, 255, 255);
        if (getStock() < getMinStock())
            warningColor = Color.rgb(255, 51, 51); //red
        else if (getStock() >= getMinStock() && getStock() <= Math.floor(getMinStock() * 1.5))
            warningColor = Color.rgb(255, 204, 0); //orange-yellow
        return warningColor;
    }
}