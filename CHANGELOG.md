# Changelog

### 9/28/2026
    - Added table "user_timetable"
        - This will have 5 attributes: timetable_id (int, pk), user_id (int, fk), location_id (int, fk), clock_in_time (datetime), clock_out_time (datetime).
    - Removed 3 attributes from table "user": location_id, clock_in_time, clock_out_time.
    - Added 1 releationship, "1 to many" between tables "user" and "user_timetable".
    - Changed the "perms" attribute type from string to enum type "user_perm".
    - Removed 1 attribute from table "receipt": total_cost.
        - This was done since the attribute can be computed.

    - Add table "inventory"
        - This will have 7 attributes: inventory_id (int, pk), name (string), nutrition (string), stock (int), next_shipment (datetime), shelf_life (datetime), unit_size (float).

    - Added table "ingredient_list"
        - This will have 2 attributes: item_id (int, fk), inventory_id (int, fk).
