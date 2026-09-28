-- 52 Weeks of Sale History

SELECT
    DATE_TRUNC('week', receipt_timestamp) AS week,
    COUNT(*) AS order_count
FROM Receipt 
WHERE receipt_timestamp >= CURRENT_DATE - INTERVAL '52 weeks'
GROUP BY DATE_TRUNC('week', receipt_timestamp)
ORDER BY week;

-- Orders fall within operating hours

SELECT 
    EXTRACT(HOUR FROM receipt_timestamp) AS hour,
    COUNT(*) as order_count,
    SUM(total_cost) AS total_sales
FROM Receipt
GROUP BY EXTRACT(HOUR FROM receipt_timestamp)
ORDER BY hour;

-- 2 peak days

SELECT
    DATE_TRUNC('day', receipt_timestamp)::date AS day,
    SUM(total_cost) AS total_sales
FROM Receipt
GROUP BY DATE_TRUNC('day', receipt_timestamp)
ORDER BY total_sales DESC
LIMIT 10;

-- Inventory items for 20 menu items    

SELECT
    c.name AS menu_item,
    COUNT(i.item_id) AS inventory_item_count
FROM category c
LEFT JOIN item i ON i.category_id = c.category_id
GROUP BY c.name
ORDER BY c.name;

-- Best of the Worst

SELECT
    i.name AS best_selling_item,
    SUM(ri.quantity) AS units_sold
FROM receipt r
JOIN receipt_item ri ON ri.receipt_id = r.receipt_id
JOIN item i ON i.item_id = ri.item_id
WHERE DATE(r.receipt_timestamp) = (
    SELECT DATE(receipt_timestamp)
    FROM receipt
    GROUP BY DATE(receipt_timestamp)
    ORDER BY SUM(total_cost) ASC
    LIMIT 1
)
GROUP BY i.name
ORDER BY units_sold DESC
LIMIT 1;

-- Best of the best

SELECT
    i.name AS best_selling_item,
    SUM(ri.quantity) AS units_sold
FROM receipt r
JOIN receipt_item ri ON ri.receipt_id = r.receipt_id
JOIN item i ON i.item_id = ri.item_id
GROUP BY i.name
ORDER BY units_sold DESC
LIMIT 1;

-- Hour of the day with most sales

SELECT
    EXTRACT(HOUR FROM receipt_timestamp) AS hour,
    SUM(total_cost) AS total_sales
FROM receipt
GROUP BY EXTRACT(HOUR FROM receipt_timestamp)
ORDER BY total_sales DESC
LIMIT 1;

-- Top 5 best selling items

SELECT
    i.name AS best_selling_item,
    SUM(ri.quantity) AS units_sold
FROM receipt_item ri
JOIN item i ON i.item_id = ri.item_id
GROUP BY i.name
ORDER BY units_sold DESC
LIMIT 5;

