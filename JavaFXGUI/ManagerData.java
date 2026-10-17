import java.util.List;
/**
 * This class holds all the data for the manager controller, including sales, stock, receipts, and menu information.
 * @author Pablo Almanza
 */
public final class ManagerData {
    private final List<SalesRecord> sales;
    private final List<StockRecord> stock;
    private final List<StockGraphRecord> productUsed;
    private final List<ReceiptRecord> receipts;
    private final List<MenuRecord> menu;

    public ManagerData(List<SalesRecord> sales, List<StockRecord> stock, List<StockGraphRecord> productUsed,
            List<ReceiptRecord> receipts, List<MenuRecord> menu) {
        this.sales = sales;
        this.stock = stock;
        this.productUsed = productUsed;
        this.receipts = receipts;
        this.menu = menu;
    }

    //Getter functions
    public List<SalesRecord> getSales(){
        return sales;
    }
    public List<StockRecord> getStock(){
        return stock;
    }
    public List<StockGraphRecord> getStockGraphRecord(){
        return productUsed;
    }
    public List<ReceiptRecord> getReceipts(){
        return receipts;
    }
    public List<MenuRecord> getMenu(){
        return menu;
    }
}