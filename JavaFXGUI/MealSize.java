import java.math.BigDecimal;

/**
 * Represents meal sizes and their discounts
 *
 * @author Sharaf Mehmood
 */
public enum MealSize {
    /** A bowl containing one main. */
    BOWL("Bowl", 1, new BigDecimal("0.4175")),
    /** A plate containing two mains. */
    PLATE("Plate", 2, new BigDecimal("0.5365")),
    /** A bigger plate containing three mains. */
    BIGGER_PLATE("Bigger Plate", 3, new BigDecimal("0.58775"));

    private final String label;
    private final int mains;
    private final BigDecimal discount;

    MealSize(String label, int mains, BigDecimal discount) {
        this.label = label;
        this.mains = mains;
        this.discount = discount;
    }

    /**
     * Returns the display label for this meal size.
     *
     * @return the meal size label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the number of mains included in this meal size.
     *
     * @return the number of mains
     */
    public int getMains() {
        return mains;
    }

    /**
     * Returns the discount multiplier used for this meal size.
     *
     * @return the discount multiplier
     */
    public BigDecimal getDiscount() {
        return discount;
    }
}
