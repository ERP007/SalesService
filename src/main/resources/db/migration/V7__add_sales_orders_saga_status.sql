-- 비동기 재고 saga 진행 상태(transport 관심사). 비즈니스 status와 분리.
-- 기존 행은 saga 미시작이므로 NONE으로 채운다.
ALTER TABLE sales_orders ADD COLUMN saga_status VARCHAR(255) NOT NULL DEFAULT 'NONE';
