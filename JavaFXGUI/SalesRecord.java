/**
 * This class represents a sales record, including the name of the item and the quantity sold.
 * @author Pablo Almanza
 */
public final class SalesRecord {
    private final String name;
    private final long quantity;

    public SalesRecord(String name, long quantity) {
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