import java.math.BigDecimal;
/**
 * This class represents a receipt record, including the receipt ID, item name, quantity, line total, item ID, and timestamp.
 * @author Pablo Almanza
 */
public final class ReceiptRecord {
    private final int receiptID;
    private final String itemName;
    private final int quantity;
    private final BigDecimal lineTotal;
    private final int itemID;
    private final String timestamp;

    public ReceiptRecord(int receiptID, int itemID, String itemName, int quantity, BigDecimal lineTotal, String timestamp) {
        this.receiptID = receiptID;
        this.itemName = itemName;
        this.quantity = quantity;
        this.lineTotal = lineTotal;
        this.itemID = itemID;
        this.timestamp = timestamp;
    }

    //getter functions
    public int getReceiptID(){
        return receiptID;
    }
    public String getItemName(){
        return itemName;
    }
    public int getQuantity(){
        return quantity;
    }
    public BigDecimal getLineTotal(){
        return lineTotal;
    }
    public int getItemID(){
        return itemID;
    }
    public String getTimestamp(){
        return timestamp;
    }
}