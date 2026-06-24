package com.fallguys.salesservice.domain.model.salesorder;

/**
 * 화면 표시용 진행 상태. 비즈니스 마일스톤({@link SalesOrderStatus})과 재고 saga 진행
 * ({@link SagaStatus})의 조합을 백엔드가 단일 값으로 파생해 노출한다. UI는 이 enum을
 * 라벨로만 매핑하고, 조합 규칙(어떤 status+saga가 어떤 진행인지)은 알 필요가 없다.
 */
public enum OrderProgress {
    DRAFT,
    REQUESTED,
    OUTBOUND_IN_PROGRESS,   // 승인 후 출고 saga 진행 중
    APPROVED,               // 출고 확정(saga DONE)
    OUTBOUND_FAILED,        // 출고 보상으로 REQUESTED 복귀(재승인 필요)
    INBOUND_IN_PROGRESS,    // 배송 후 입고 saga 진행 중
    DELIVERED,              // 입고 확정(saga DONE)
    INBOUND_FAILED,         // 입고 보상으로 APPROVED 복귀(재입고 필요)
    REJECTED,
    CANCELED;

    /** (status, saga) 조합 규칙의 단일 소스. SalesOrder·요약 매핑이 공통으로 사용한다. */
    public static OrderProgress from(SalesOrderStatus status, SagaStatus saga) {
        return switch (status) {
            case DRAFT -> DRAFT;
            case REQUESTED -> saga == SagaStatus.FAILED ? OUTBOUND_FAILED : REQUESTED;
            case APPROVED -> switch (saga) {
                case SENDING, PROCESSING -> OUTBOUND_IN_PROGRESS;
                case FAILED -> INBOUND_FAILED;
                case NONE, DONE -> APPROVED;
            };
            case DELIVERED -> saga.inProgress() ? INBOUND_IN_PROGRESS : DELIVERED;
            case REJECTED -> REJECTED;
            case CANCELED -> CANCELED;
        };
    }
}
