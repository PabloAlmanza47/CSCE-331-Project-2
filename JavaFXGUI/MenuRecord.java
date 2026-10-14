
/**
 * This class represents a menu record, including the item ID, name, and price.
 * @author Pablo Almanza
 */
public final class MenuRecord {
    private final int itemID;
    private final String name;
    private final double price;

    public MenuRecord(int itemID, String name, double price) {
        this.itemID = itemID;
        this.name = name;
        this.price = price;
    }

    //getter functions
    public int getItemID(){
        return itemID;
    }
    public String getName(){
        return name;
    }
    public double getPrice(){
        return price;
    }
}