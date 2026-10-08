# Changelog

### 9/28/2026 — Phase 2 schema changes

- Added user_timetable with timetable_id, user_id, location_id, clock_in_time, and clock_out_time. Shift times and locations now belong to individual timetable records, allowing a user to have more than one shift. Removed location_id, clock_in_time, and clock_out_time from app_user and removed the direct LOCATION–USER relationship from the ERD.

- Changed app_user.perms from a string to the user_perm enum so permission values match the defined cashier and manager roles.

- Removed receipt.total_cost because it can be calculated from the quantities and prices recorded in receipt_item.

- Added inventory to record ingredient stock, shipment timing, shelf life, and unit size separately from menu items.

- Added ingredient_list to associate menu items with their inventory ingredients.

- Restored item.unit_size in the SQL schema so it matches the updated ERD’s menu item fields.

### 10/7/2026 — Phase 3 GUI changes

- Added "Change View" buttons to easily switch in between cashier and manager view.

- Added "Close" buttons to allow users to close out of the application.

- Formatted Manager GUI to be tabs to easily switch views for different purposes.

- Added a new tab to Manager GUI called "Menu" to allow the manager to increase/decrease prices and add/remove menu items.
