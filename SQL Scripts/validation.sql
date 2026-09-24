-- 52 Weeks of Sale History

SELECT
    DATE_TRUNC('week', timestamp) AS week,
    COUNT(*) AS order_count
FROM Receipt 
WHERE timestamp >= CURRENT_DATE - INTERVAL '52 weeks'
GROUP BY DATE_TRUNC('week', timestamp)
ORDER BY week;

-- Orders fall within operating hours

SELECT 
    DATE_TRUNC('hour', timestamp) AS hour,
    COUNT(*) as order_count,
    SUM(total_cost) AS total_sales
FROM Receipt
GROUP BY EXTRACT(HOUR FROM "timestamp")
ORDER BY hour;

-- 2 peak days

-- Inventory items for 20 menu items

-- Best of the Worst

--etc

--etc

--etc

