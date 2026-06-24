-- 이번 스텝의 스키마 변경 일괄:
--  1) 창고명·행위자(name/position) 스냅샷 컬럼 추가 (확정 시점 박제, DRAFT는 code만 → nullable)
--  2) desired_arrival_date 제거 (도착 희망일 기능 삭제)
--  3) 이력 행위자 name/position 스냅샷
--  4) 재고 saga 확정 대기 staging 테이블 (pending_status_change)
--  5) 목록·집계 인덱스 (entity @Index 선언과 일치)

-- 1) 발주 창고명·생성자·요청자 스냅샷
ALTER TABLE sales_orders
    ADD COLUMN from_warehouse_name   VARCHAR(255),
    ADD COLUMN to_warehouse_name     VARCHAR(255),
    ADD COLUMN created_by_name       VARCHAR(255),
    ADD COLUMN created_by_position   VARCHAR(255),
    ADD COLUMN requested_by_name     VARCHAR(255),
    ADD COLUMN requested_by_position VARCHAR(255);

-- 2) 도착 희망일 제거
ALTER TABLE sales_orders
    DROP COLUMN desired_arrival_date;

-- 3) 이력 행위자 name/position 스냅샷
ALTER TABLE sales_order_status_history
    ADD COLUMN actor_name     VARCHAR(255),
    ADD COLUMN actor_position VARCHAR(255);

-- 4) saga 확정 대기 상태 변경(staging). 발주당 1건이므로 so_code가 PK.
--    saga DONE 시 이력으로 승격 후 삭제, FAILED 시 삭제된다.
CREATE TABLE pending_status_change (
    so_code        VARCHAR(255)               NOT NULL,
    status         VARCHAR(255)               NOT NULL,
    actor_code     VARCHAR(255)               NOT NULL,
    actor_name     VARCHAR(255),
    actor_position VARCHAR(255),
    payload        JSONB,
    occurred_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_pending_status_change PRIMARY KEY (so_code)
);

-- 5) 목록·집계 인덱스
--    지점 목록: from_warehouse_code 필터 + requested_at 정렬/범위
CREATE INDEX idx_so_from_warehouse_requested_at ON sales_orders (from_warehouse_code, requested_at);
--    본사 목록: 창고 미지정 시 requested_at 범위/정렬
CREATE INDEX idx_so_requested_at ON sales_orders (requested_at);
--    상태 집계(KPI)·status IN 필터
CREATE INDEX idx_so_status ON sales_orders (status);
--    라인 묶음 조회(@Formula itemCount·EXISTS 검색·라인 로드)
CREATE INDEX idx_sol_so_code ON sales_order_lines (so_code);
