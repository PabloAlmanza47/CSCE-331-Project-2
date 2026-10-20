import java.math.BigDecimal;
/**
 * Represents meal sizes and their discounts
 * @author Sharaf Mehmood
 */ 
public enum MealSize {
    BOWL("Bowl", 1, new BigDecimal("0.39068")),
    PLATE("Plate", 2, new BigDecimal("0.54839")),
    BIGGER_PLATE("Bigger Plate", 3, new BigDecimal("0.61598"));
 
    private final String label;
    private final int mains;
    private final BigDecimal discount;

    /**
    * Represents meal sizes and their discounts
    * @param label label for the size
    * @param mains amount of mains in the size
    * @param discount the % discount you get when ordering this size 
    * @author Sharaf Mehmood
    */ 
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