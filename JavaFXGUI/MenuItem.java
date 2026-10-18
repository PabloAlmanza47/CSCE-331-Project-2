import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * This class represents a menu item with its properties and provides getters and setters for each property.
 * @author Ashley Hoang
 */
public class MenuItem {
    private final IntegerProperty itemID = new SimpleIntegerProperty();
    private final StringProperty itemName = new SimpleStringProperty();
    private final DoubleProperty itemPrice = new SimpleDoubleProperty();

    /**
     * Constructs a new MenuItem with the specified ID, name, and price.
     * @param itemID The ID for the menu item.
     * @param itemName The name of the menu item.
     * @param itemPrice The price of the menu item.
     * @author Ashley Hoang
     */
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
}
