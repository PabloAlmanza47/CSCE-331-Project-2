import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

// This class represents a menu item with its properties and provides getters and setters for each property.
public class MenuItem {
    private final IntegerProperty itemID = new SimpleIntegerProperty();
    private final StringProperty itemName = new SimpleStringProperty();
    private final IntegerProperty itemPrice = new SimpleIntegerProperty();

    public MenuItem(int itemID, String itemName, int itemPrice) {
        this.itemID.set(itemID);
        this.itemName.set(itemName);
        this.itemPrice.set(itemPrice);
    }

    // Getters and setters for each field
    public int getItemID() { return itemID.get(); }
    public String getItemName() { return itemName.get(); }
    public int getItemPrice() { return itemPrice.get(); }

    public void setPrice(int value) { itemPrice.set(value); }
    public IntegerProperty itemPriceProperty() { return itemPrice; }
}