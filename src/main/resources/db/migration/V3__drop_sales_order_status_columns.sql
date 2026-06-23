-- 상태 변경 부가 데이터를 sales_order_status_history로 단일화하면서 sales_orders의 평탄화 컬럼을 제거한다.
-- creation(created_by/at)·request(requested_by/at)는 목록·정렬·검증에 쓰이는 운영 컬럼이라 유지한다.

-- 송장 번호 유니크 제약 제거(중복 검증 폐지)
ALTER TABLE sales_orders DROP CONSTRAINT IF EXISTS uq_sales_orders_invoice_number;

-- approval
ALTER TABLE sales_orders DROP COLUMN approved_by;
ALTER TABLE sales_orders DROP COLUMN approved_at;
ALTER TABLE sales_orders DROP COLUMN approved_date;
ALTER TABLE sales_orders DROP COLUMN carrier_type;
ALTER TABLE sales_orders DROP COLUMN invoice_number;

-- rejection
ALTER TABLE sales_orders DROP COLUMN rejected_by;
ALTER TABLE sales_orders DROP COLUMN rejected_at;
ALTER TABLE sales_orders DROP COLUMN reject_reason_category;
ALTER TABLE sales_orders DROP COLUMN reject_reason_memo;

-- delivery
ALTER TABLE sales_orders DROP COLUMN delivered_by;
ALTER TABLE sales_orders DROP COLUMN delivered_date;
ALTER TABLE sales_orders DROP COLUMN delivered_at;
ALTER TABLE sales_orders DROP COLUMN diff_reason_category;
ALTER TABLE sales_orders DROP COLUMN diff_reason_memo;

-- cancellation
ALTER TABLE sales_orders DROP COLUMN canceled_by;
ALTER TABLE sales_orders DROP COLUMN canceled_at;
ALTER TABLE sales_orders DROP COLUMN cancel_reason;
