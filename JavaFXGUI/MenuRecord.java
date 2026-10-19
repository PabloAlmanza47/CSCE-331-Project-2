import java.math.BigDecimal;

/**
 * This class represents a menu record, including the item ID, name, and price.
 *
 * @author Pablo Almanza
 */

public final class MenuRecord {
    private final int itemID;
    private final BigDecimal price;
    private final String name;
    private final int categoryID;

    /**
     * Creates a menu record with the item's database ID, price, name, and category.
     *
     * @param itemID     the item's database ID
     * @param price      the item's price
     * @param name       the item's name
     * @param categoryID the item's category ID
     */

    public MenuRecord(int itemID, BigDecimal price, String name, int categoryID) {
        this.itemID = itemID;
        this.price = price;
        this.name = name;
        this.categoryID = categoryID;
    }

    /**
     * Returns the menu item's database ID.
     *
     * @return the menu item ID
     */
    public int getItemID() {
        return itemID;
    }

    /**
     * Returns the menu item's price.
     *
     * @return the menu item price
     */
    public BigDecimal getPrice() {
        return price;
    }

    /**
     * Returns the menu item's name.
     *
     * @return the menu item name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the menu item's category ID.
     *
     * @return the menu item category ID
     */
    public int getCategoryID() {
        return categoryID;
    }
}
