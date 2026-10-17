import java.math.BigDecimal;

/**
 * This class represents a menu record, including the item ID, name, and price.
 * @author // Pablo Almanza
 */

public final class MenuRecord {
    private final int itemID;
    private final BigDecimal price;
    private final String name;
    private final int categoryID;

    public MenuRecord(int itemID, BigDecimal price, String name, int categoryID) {
        this.itemID = itemID;
        this.price = price;
        this.name = name;
        this.categoryID = categoryID;
    }

    public int getItemID(){
        return itemID;
    }

    public BigDecimal getPrice(){
        return price;
    }

    public String getName(){
        return name;
    }

    public int getCategoryID(){
        return categoryID;
    }
}
