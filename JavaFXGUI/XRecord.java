/**
 * This class represents a sales record per hour (x report), including the hour and the quantity sold.
 * @author Ashley Hoang
 */
public final class XRecord {
    private final String hour;
    private final long quantity;
    private final double revenue;

    public XRecord(String hour, long quantity, double revenue) {
        this.hour = hour;
        this.quantity = quantity;
        this.revenue = revenue;
    }

    //getter functions
    public String getHour(){
        return hour;
    }
    public long getQuantity(){
        return quantity;
    }
    public double getRevenue(){
        return revenue;
    }
}