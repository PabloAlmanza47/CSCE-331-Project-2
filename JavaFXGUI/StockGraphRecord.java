/**
 * This class represents a product/stock used record, including the name of the item and the quantity used.
 * @author Ashley Hoang
 */
public final class StockGraphRecord {
    private final String name;
    private final long quantity;
    private final int id;

    public StockGraphRecord(int id, String name, long quantity) {
        this.name = name;
        this.quantity = quantity;
        this.id = id;
    }

    //getter functions
    public String getName(){
        return name;
    }
    public long getQuantity(){
        return quantity;
    }
    public int getID(){
        return id;
    }
}