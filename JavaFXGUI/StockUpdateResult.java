/**
 * This class represents the result of a stock update operation, including the new stock value, whether the update was successful, and whether the inventory record was missing.
 * @author Pablo Almanza
 */
public final class StockUpdateResult {
    private final Integer stock;
    private final boolean updated;    
    private final boolean missing;
    public StockUpdateResult(Integer stock, boolean updated, boolean missing) {
        this.stock = stock;
        this.updated = updated;
        this.missing = missing;
    }

    public Integer getStock() {
        return stock;
    }

    public boolean isUpdated() {
        return updated;
    }

    public boolean isMissing() {
        return missing;
    }
}