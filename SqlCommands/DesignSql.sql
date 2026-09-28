-- Drop tables if they already exist
DROP TABLE IF EXISTS receipt_item;
DROP TABLE IF EXISTS receipt;
DROP TABLE IF EXISTS ingredient_list;
DROP TABLE IF EXISTS item;
DROP TABLE IF EXISTS inventory;
DROP TABLE IF EXISTS user_timetable;
DROP TABLE IF EXISTS app_user;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS location;

-- ENUM CREATION
DO $$
BEGIN
    CREATE TYPE user_perm AS ENUM ('Cashier', 'Manager');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- LOCATION table
CREATE TABLE location (
    location_id INTEGER PRIMARY KEY,
    name VARCHAR(32) NOT NULL,
    city VARCHAR(32) NOT NULL,
    state VARCHAR(16) NOT NULL
);

-- CATEGORY table
CREATE TABLE category (
    category_id INTEGER PRIMARY KEY,
    name VARCHAR(32) NOT NULL,
    allergy_info TEXT
);

-- USER table
-- Named app_user to avoid PostgreSQL reserved keyword issues
CREATE TABLE app_user (
    user_id INTEGER PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    password VARCHAR(255) NOT NULL,
    perms user_perm NOT NULL
);

-- USER_TIMETABLE table
CREATE TABLE user_timetable (
    timetable_id INTEGER PRIMARY KEY,
    user_id INTEGER NOT NULL,
    location_id INTEGER NOT NULL,
    clock_in_time TIMESTAMP,
    clock_out_time TIMESTAMP,

    CONSTRAINT fk_user_id
        FOREIGN KEY (user_id)
        REFERENCES app_user(user_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_user_location
        FOREIGN KEY (location_id)
        REFERENCES location(location_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

-- ITEM table
CREATE TABLE item (
    item_id INTEGER PRIMARY KEY,
    category_id INTEGER NOT NULL,
    nutrition TEXT,
    price NUMERIC(10, 2) NOT NULL,
    name VARCHAR(100) NOT NULL,

    CONSTRAINT fk_item_category
        FOREIGN KEY (category_id)
        REFERENCES category(category_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT chk_item_price CHECK (price >= 0)
);

-- INVENTORY table
CREATE TABLE inventory (
    inventory_id INTEGER PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    nutrition VARCHAR(128),
    stock INT NOT NULL DEFAULT 0,
    min_stock INT NOT NULL DEFAULT 0,
    next_shipment TIMESTAMP,
    shelf_life TIMESTAMP,
    unit_size FLOAT,

    CONSTRAINT chk_item_stock CHECK (stock >= 0),
    CONSTRAINT chk_item_min_stock CHECK (min_stock >= 0)
);

-- INGREDIENT_LIST table
CREATE TABLE ingredient_list (
    item_id INTEGER NOT NULL,
    inventory_id INTEGER NOT NULL,

    CONSTRAINT fk_item_id
        FOREIGN KEY (item_id)
        REFERENCES item(item_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_inventory_id
        FOREIGN KEY (inventory_id)
        REFERENCES inventory(inventory_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

-- RECEIPT table
CREATE TABLE receipt (
    receipt_id INTEGER PRIMARY KEY,
    location_id INTEGER,
    user_id INTEGER,
    receipt_timestamp TIMESTAMP NOT NULL,

    CONSTRAINT fk_receipt_location
        FOREIGN KEY (location_id)
        REFERENCES location(location_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_receipt_user
        FOREIGN KEY (user_id)
        REFERENCES app_user(user_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

-- RECEIPT_ITEM table
CREATE TABLE receipt_item (
    receipt_item_id INTEGER PRIMARY KEY,
    receipt_id INTEGER NOT NULL,
    item_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL,
    price_at_sale NUMERIC(10, 2) NOT NULL,

    CONSTRAINT fk_receipt_item_receipt
        FOREIGN KEY (receipt_id)
        REFERENCES receipt(receipt_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_receipt_item_item
        FOREIGN KEY (item_id)
        REFERENCES item(item_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT chk_receipt_item_quantity CHECK (quantity > 0),
    CONSTRAINT chk_receipt_item_price CHECK (price_at_sale >= 0)
);

-- Indexes for foreign keys
CREATE INDEX idx_user_location_id
    ON user_timetable(location_id);

CREATE INDEX idx_item_category_id
    ON item(category_id);

CREATE INDEX idx_receipt_location_id
    ON receipt(location_id);

CREATE INDEX idx_receipt_user_id
    ON receipt(user_id);

CREATE INDEX idx_receipt_item_receipt_id
    ON receipt_item(receipt_id);

CREATE INDEX idx_receipt_item_item_id
    ON receipt_item(item_id);