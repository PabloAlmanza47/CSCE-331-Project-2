import javafx.scene.control.Label;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

// This class represents a custom ListCell for displaying MenuItem objects in a ListView. 
public class MenuListCell extends ListCell<MenuItem> {
    private final HBox content;
    private final Label infoLabel;
    private final Button increaseBtn;
    private final Button decreaseBtn;
    private final Button removeBtn;

    private final managerController controller;

    public MenuListCell(managerController controller) {
        this.controller = controller;
        infoLabel = new Label();
        increaseBtn = new Button("Increase Price");
        decreaseBtn = new Button("Decrease Price");
        removeBtn = new Button("Remove Item");

        // Set up button actions to modify price and update the display
        increaseBtn.setOnAction(e -> {
            MenuItem item = getItem();
            if (item != null) {
                double newPrice = item.getItemPrice() + 1.00;
                item.setPrice(newPrice);
                updateDisplay();
                controller.updatePriceInDatabase(item.getItemID(), newPrice);
            }
        });
        decreaseBtn.setOnAction(e -> {
            MenuItem item = getItem();
            if (item != null) {
                double newPrice = Math.max(0.0, item.getItemPrice() - 1.00);
                item.setPrice(newPrice);
                updateDisplay();
                controller.updatePriceInDatabase(item.getItemID(), newPrice);
            }
        });
        removeBtn.setOnAction(e -> {
            MenuItem item = getItem();
            if (item != null) {
                controller.removeMenuItemFromDatabase(item);
                updateDisplay();
            }
        });

        // Set HBox grow priorities to ensure proper layout
        HBox.setHgrow(increaseBtn, Priority.NEVER);
        HBox.setHgrow(decreaseBtn, Priority.NEVER);
        HBox.setHgrow(removeBtn, Priority.NEVER);
        HBox.setHgrow(infoLabel, Priority.ALWAYS);

        content = new HBox(10, infoLabel, increaseBtn, decreaseBtn, removeBtn);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(5));
    }

    /**
     * Updates the display of the ListCell based on the current MenuItem.
     *
     * @author Ashley Hoang
     */
    private void updateDisplay() {
        MenuItem item = getItem();
        if (item != null) {
            infoLabel.setText(String.format(
                    "#%d %s:\n- Price: $%.2f",
                    item.getItemID(), item.getItemName(), item.getItemPrice()));
        }
    }

    /**
     * Overrides the updateItem method to update the ListCell's content when the
     * item changes.
     *
     * @param item  The new MenuItem to display.
     * @param empty Whether the cell is empty.
     * @author Ashley Hoang
     */
    @Override
    protected void updateItem(MenuItem item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            updateDisplay();
            setGraphic(content);
        }
    }
}
