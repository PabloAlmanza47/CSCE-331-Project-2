import java.math.BigDecimal;
/**
 * Represents meal sizes and their discounts
 * @author // TODO
 */ 
public enum MealSize {
    BOWL("Bowl", 1, new BigDecimal("0.4175")),
    PLATE("Plate", 2, new BigDecimal("0.5365")),
    BIGGER_PLATE("Bigger Plate", 3, new BigDecimal("0.58775"));
 
    private final String label;
    private final int mains;
    private final BigDecimal discount;
 
    MealSize(String label, int mains, BigDecimal discount) {
        this.label = label;
        this.mains = mains;
        this.discount = discount;
    }

    public String getLabel(){
        return label;
    }

    public int getMains(){
        return mains;
    }

    public BigDecimal getDiscount(){
        return discount;
    }
}