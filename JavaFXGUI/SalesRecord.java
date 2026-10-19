/**
 * This class represents a sales record, including the name of the item and the quantity sold.
 * @author Pablo Almanza
 */
public final class SalesRecord {
    private final String name;
    private final long quantity;
    private final int id;

    public SalesRecord(int id, String name, long quantity) {
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