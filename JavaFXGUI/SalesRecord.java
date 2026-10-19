/**
 * This class represents a sales record, including the name of the item and the
 * quantity sold.
 *
 * @author Pablo Almanza
 */
public final class SalesRecord {
    private final String name;
    private final long quantity;
    private final int id;

    /**
     * Creates a sales record for an item and its quantity sold.
     *
     * @param id       the item's database ID
     * @param name     the item's name
     * @param quantity the quantity of the item sold
     */

    public SalesRecord(int id, String name, long quantity) {
        this.name = name;
        this.quantity = quantity;
        this.id = id;
    }

    // getter functions
    /**
     * Returns the item name.
     *
     * @return the item name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the quantity sold.
     *
     * @return the quantity sold
     */
    public long getQuantity() {
        return quantity;
    }

    /**
     * Returns the item's database ID.
     *
     * @return the item ID
     */
    public int getID() {
        return id;
    }
}
