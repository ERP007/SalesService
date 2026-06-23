ALTER TABLE sales_order_lines RENAME COLUMN requested_quantity TO quantity;

ALTER TABLE sales_order_lines
    DROP COLUMN approved_quantity,
    DROP COLUMN delivered_quantity;
