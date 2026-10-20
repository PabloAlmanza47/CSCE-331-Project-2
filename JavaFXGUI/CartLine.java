import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.control.CheckBox;


/**
 * Class representing a cart line
 * @author Sharaf Mehmood
 */
public final class CartLine {
    private final String key;
    private final String label;
    private final BigDecimal unitPrice;
    private final List<MenuRecord> components;
    private int quantity = 1;
    private CheckBox row;

    /**
    * Cart Line constructor 
    * @param key key to the cart line
    * @param label the descriptor of the entire cart line
    * @param unitPrice the total price of the cart line
    * @param components list of the menu records included in the cart line
    * @author Sharaf Mehmood
    */
    public CartLine(String key, String label, BigDecimal unitPrice, List<MenuRecord> components) {
        this.key = key;
        this.label = label;
        this.unitPrice = unitPrice;
        this.components = components;
    }

    /**
    * Creates the cart line for an a la carte item  
    * @param record the menu record for the item
    * @return the cart line of the item
    * @author Sharaf Mehmood
    */
    public static CartLine single(MenuRecord record) {
        return new CartLine("item:" + record.getItemID(), record.getName(), record.getPrice(), List.of(record));
    }

    /**
    * Creates the cart line for a meal (bowl, plate, bigger plate)
    * @param size the menu record for the item
    * @param entrees the main dishes being ordered
    * @param side the side dish being ordered
    * @return the cart line of the meal
    * @author Sharaf Mehmood
    */
    public static CartLine meal(MealSize size, List<MenuRecord> entrees, MenuRecord side) {
        List<MenuRecord> components = new ArrayList<>(entrees);
        components.add(side);
        BigDecimal listTotal = BigDecimal.ZERO;
        for (MenuRecord c : components) {
            listTotal = listTotal.add(c.getPrice());
        }
        BigDecimal price = listTotal.multiply(BigDecimal.ONE.subtract(size.getDiscount())).setScale(2, RoundingMode.HALF_UP);
        return new CartLine(null, size.getLabel(), price, components);
    }

    /**
    * Shows the total price of the cart line
    * @return price of the cart line
    * @author Sharaf Mehmood
    */
    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /**
    * Calculates the amount to discount 
    * @return the amount to discount off the order
    * @author Sharaf Mehmood
    */
    public BigDecimal discount() {
        BigDecimal listTotal = BigDecimal.ZERO;
        for (MenuRecord component : components) listTotal = listTotal.add(component.getPrice());
        return listTotal.subtract(unitPrice);
    }

     /**
    * Describes the entire cart line 
    * @return the cart line formatted into a string
    * @author Sharaf Mehmood
    */
    public String describe() {
        String text = quantity + "x " + label;
        if (components.size() > 1) {
            text += " (" + components.stream().map(MenuRecord::getName).collect(Collectors.joining(", ")) + ")";
        }
        return text + " $%.2f".formatted(lineTotal());
    }
    
    public String getKey() { 
        return key;
    }

    public String getLabel() {
        return label;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public List<MenuRecord> getComponents() {
        return components;
    }

    public CheckBox getRow() {
        return row;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int q) {
        quantity = q;
    }

    public void setRow(CheckBox r) {
        row = r;
    }
}
