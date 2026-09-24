-- 52 Weeks of Sale History

SELECT
    DATE_TRUNC('week', timestamp) AS week,
    COUNT(*) AS order_count,
FROM Receipt 
WHERE timestamp >= CURRENT_DATE - INTERVAL '52 weeks'
GROUP BY DATE_TRUNC('week', timestamp)
ORDER BY week;

-- Orders fall within operating hours