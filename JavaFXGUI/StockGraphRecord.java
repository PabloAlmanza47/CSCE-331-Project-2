/**
 * This class represents a product/stock used record, including the name of the item and the quantity used.
 * @author Ashley Hoang
 */
public final class StockGraphRecord {
    private final String name;
    private final long quantity;
    private final int id;

    /**
     * Creates a stock usage record for graphing.
     *
     * @param id       the inventory item's database ID
     * @param name     the inventory item's name
     * @param quantity the quantity used
     */
    public StockGraphRecord(int id, String name, long quantity) {
        this.name = name;
        this.quantity = quantity;
        this.id = id;
    }

    //getter functions
    /**
     * Returns the inventory item name.
     *
     * @return the inventory item name
     */
    public String getName(){
        return name;
    }
    /**
     * Returns the quantity used.
     *
     * @return the quantity used
     */
    public long getQuantity(){
        return quantity;
    }
    /**
     * Returns the inventory item's database ID.
     *
     * @return the inventory item ID
     */
    public int getID(){
        return id;
    }
}
