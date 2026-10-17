/**
 * This class represents a product/stock used record, including the name of the item and the quantity used.
 * @author Ashley Hoang
 */
public final class StockGraphRecord {
    private final String name;
    private final long quantity;

    public StockGraphRecord(String name, long quantity) {
        this.name = name;
        this.quantity = quantity;
    }

    //getter functions
    public String getName(){
        return name;
    }
    public long getQuantity(){
        return quantity;
    }
}