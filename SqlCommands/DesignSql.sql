-- Drop tables if they already exist
DROP TABLE IF EXISTS receipt_item;
DROP TABLE IF EXISTS receipt;
DROP TABLE IF EXISTS item;
DROP TABLE IF EXISTS app_user;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS location;

-- LOCATION table
CREATE TABLE location (
    location_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(32) NOT NULL,
    city VARCHAR(32) NOT NULL,
    state VARCHAR(16) NOT NULL
);

-- CATEGORY table
CREATE TABLE category (
    category_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(32) NOT NULL,
    allergy_info TEXT
);

-- USER table
-- Named app_user to avoid PostgreSQL reserved keyword issues
CREATE TABLE app_user (
    user_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    location_id INTEGER NOT NULL,
    password VARCHAR(255) NOT NULL,
    perms VARCHAR(50) NOT NULL,
    clock_in_time TIMESTAMP,
    clock_out_time TIMESTAMP,

    CONSTRAINT fk_user_location
        FOREIGN KEY (location_id)
        REFERENCES location(location_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

-- ITEM table
CREATE TABLE item (
    item_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_id INTEGER NOT NULL,
    nutrition TEXT,
    price NUMERIC(10, 2) NOT NULL,
    stock INTEGER NOT NULL DEFAULT 0,
    next_shipment TIMESTAMP,
    unit_size VARCHAR(50),
    name VARCHAR(100) NOT NULL,

    CONSTRAINT fk_item_category
        FOREIGN KEY (category_id)
        REFERENCES category(category_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT chk_item_price CHECK (price >= 0),
    CONSTRAINT chk_item_stock CHECK (stock >= 0)
);

-- RECEIPT table
CREATE TABLE receipt (
    receipt_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    location_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    receipt_timestamp TIMESTAMP NOT NULL,
    total_cost NUMERIC(10, 2) NOT NULL,

    CONSTRAINT fk_receipt_location
        FOREIGN KEY (location_id)
        REFERENCES location(location_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_receipt_user
        FOREIGN KEY (user_id)
        REFERENCES app_user(user_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT chk_receipt_total_cost CHECK (total_cost >= 0)
);

-- RECEIPT_ITEM table
CREATE TABLE receipt_item (
    receipt_item_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
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
    ON app_user(location_id);

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