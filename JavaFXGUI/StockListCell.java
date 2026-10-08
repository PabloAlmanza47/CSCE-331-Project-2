import javafx.scene.control.Label;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

// This class represents a custom ListCell for displaying StockItem objects in a ListView. 
// It provides buttons to increase or decrease the stock of the item and updates the display accordingly.
public class StockListCell extends ListCell<StockItem> {
    private final HBox content;
    private final Label infoLabel;
    private final Button addBtn;
    private final Button removeBtn;

    private final managerController controller;

    public StockListCell(managerController controller) {
        this.controller = controller;
        infoLabel = new Label();
        addBtn = new Button("Add Stock");
        removeBtn = new Button("Remove Stock");

        // Set up button actions to modify stock and update the display
        addBtn.setOnAction(e -> {
            StockItem item = getItem();
            if (item != null) {
                managerController.StockUpdateResult result = controller.updateStockInDatabase(item.getInventoryID(), 1);
                if (result.getStock() != null) {
                    int confirmedStock = result.getStock();
                    item.setStock(confirmedStock);
                    updateDisplay();
                    if (!result.isUpdated()) controller.showStockUpdateFailure(confirmedStock);
                } else if (result.isMissing()) {
                    getListView().getItems().remove(item);
                    controller.showMissingInventoryError();
                }
            }
        });
        removeBtn.setOnAction(e -> {
            StockItem item = getItem();
            if (item != null && item.getStock() > 0) {
                managerController.StockUpdateResult result = controller.updateStockInDatabase(item.getInventoryID(), -1);
                if (result.getStock() != null) {
                    int confirmedStock = result.getStock();
                    item.setStock(confirmedStock);
                    updateDisplay();
                    if (!result.isUpdated()) controller.showStockUpdateFailure(confirmedStock);
                } else if (result.isMissing()) {
                    getListView().getItems().remove(item);
                    controller.showMissingInventoryError();
                }
            }
        });

        // Set HBox grow priorities to ensure proper layout
        HBox.setHgrow(addBtn, Priority.NEVER);
        HBox.setHgrow(removeBtn, Priority.NEVER);
        HBox.setHgrow(infoLabel, Priority.ALWAYS);

        content = new HBox(10, infoLabel, addBtn, removeBtn);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(5));
    }

    /**
     * Updates the display of the ListCell based on the current StockItem.
     * @author Ashley Hoang
     */
    private void updateDisplay() {
        StockItem item = getItem();
        if (item != null) {
            infoLabel.setText(String.format(
                "#%d %s:\n- Stock: %d\n- Minimum Stock: %d\n- Next Shipment: %s\n- Shelf Life: %s",
                item.getInventoryID(), item.getName(), item.getStock(),
                item.getMinStock(), item.getNextShipment(), item.getShelfLife()
            ));
        }
    }

    /**
     * Overrides the updateItem method to update the ListCell's content when the item changes.
     * @param item The new StockItem to display.
     * @param empty Whether the cell is empty.
     * @author Ashley Hoang
     */
    @Override
    protected void updateItem(StockItem item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            updateDisplay();
            setGraphic(content);
        }
    }
}
