/**
 * This class represents a stock record, including the inventory ID, name, stock level, minimum stock level, next shipment date, and shelf life.
 * @author Pablo Almanza
 */
public final class StockRecord {
    private final int inventoryID;
    private final String name;
    private final int stock;
    private final int minStock;
    private final String nextShipment;
    private final String shelfLife;

    public StockRecord(int inventoryID, String name, int stock, int minStock,
            String nextShipment, String shelfLife) {
        this.inventoryID = inventoryID;
        this.name = name;
        this.stock = stock;
        this.minStock = minStock;
        this.nextShipment = nextShipment;
        this.shelfLife = shelfLife;
    }

    //getter functions
    public int getInventoryID(){
        return inventoryID;
    }
    public String getName(){
        return name;
    }
    public int getStock(){
        return stock;
    }
    public int getMinStock(){
        return minStock;
    }
    public String getNextShipment(){
        return nextShipment;
    }
    public String getShelfLife(){
        return shelfLife;
    }
}