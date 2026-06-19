CREATE INDEX idx_so_status_history_so_code_status_created_at
    ON sales_order_status_history (so_code, status, created_at DESC);
