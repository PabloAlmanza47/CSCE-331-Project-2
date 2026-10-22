package backend.database;

import backend.models.MenuRecord;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/** Reads cashier menu data from PostgreSQL. */
public final class CashierRepository {
    public List<MenuRecord> loadMenu() throws Exception {
        List<MenuRecord> menu = new ArrayList<>();
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT name, price, item_id, category_id FROM item "
                                + "WHERE category_id BETWEEN 1 AND 3 AND active = TRUE "
                                + "ORDER BY category_id, item_id");
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                menu.add(new MenuRecord(resultSet.getInt("item_id"), resultSet.getBigDecimal("price"),
                        resultSet.getString("name"), resultSet.getInt("category_id")));
            }
        }
        return menu;
    }
}
