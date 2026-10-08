import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

// This class represents a menu item with its properties and provides getters and setters for each property.
public class MenuItem {
    private final IntegerProperty itemID = new SimpleIntegerProperty();
    private final StringProperty itemName = new SimpleStringProperty();
    private final DoubleProperty itemPrice = new SimpleDoubleProperty();

    public MenuItem(int itemID, String itemName, double itemPrice) {
        this.itemID.set(itemID);
        this.itemName.set(itemName);
        this.itemPrice.set(itemPrice);
    }

    // Getters and setters for each field
    public int getItemID() {
        return itemID.get();
    }

    public String getItemName() {
        return itemName.get();
    }

    public double getItemPrice() {
        return itemPrice.get();
    }

    public void setPrice(double value) {
        itemPrice.set(value);
    }

    public DoubleProperty itemPriceProperty() {
        return itemPrice;
    }
}
