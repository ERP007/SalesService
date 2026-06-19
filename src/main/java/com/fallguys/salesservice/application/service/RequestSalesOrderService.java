package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.command.RequestSalesOrderCommand;
import com.fallguys.salesservice.application.port.inbound.usecase.RequestSalesOrderUseCase;
import com.fallguys.salesservice.application.port.outbound.model.ItemInfo;
import com.fallguys.salesservice.application.port.outbound.port.AppendSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadItemPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.SaveSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.VerifyWarehousePort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorderhistory.SalesOrderStatusHistory;
import com.fallguys.salesservice.domain.model.salesorderline.SalesOrderLine;
import com.fallguys.salesservice.domain.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RequestSalesOrderService implements RequestSalesOrderUseCase {

    private final LoadSalesOrderPort loadSalesOrderPort;
    private final VerifyWarehousePort verifyWarehousePort;
    private final LoadItemPort loadItemPort;
    private final SaveSalesOrderPort saveSalesOrderPort;
    private final AppendSalesOrderStatusHistoryPort appendHistoryPort;

    /**
     * DRAFT 발주를 REQUESTED로 전환한다 (기존 라인·창고·날짜 그대로 사용).
     *
     * 흐름:
     * 1) SO 존재 확인 (local DB)
     * 2) SO 소유 지점과 요청자 창고 일치 검증 (local)
     * 3) 중복 부품 코드 검증 (local, 기존 라인 기준)
     * 4) 도착 희망일 범위 검증 (local) — 오늘 초과 ~ 60일 이내
     * 5) 지점 창고(fromWarehouseCode) 활성 검증 (Inventory 서비스)
     * 5-1) 본사 창고(toWarehouseCode) 활성 검증 (Inventory 서비스)
     * 6) 부품 존재 확인 및 스냅샷 수집 (Item 서비스)
     * 7) 기존 라인을 스냅샷으로 재구성 후 도메인 상태 전환 및 저장
     *
     * 트랜잭션: 쓰기. 저장이 한 트랜잭션으로 묶이며 예외 시 전체 롤백.
     * 외부 서비스 호출(5~6)이 트랜잭션 내에 포함되어 DB 커넥션 점유 시간이 늘어남.
     * 추후 외부 호출을 트랜잭션 진입 전으로 분리하는 리팩토링 고려.
     *
     * 예외:
     * - HQ 계열 또는 미허용 역할: ForbiddenException (ER-403, 403)
     * - SO 미존재: ResourceNotFoundException (SO-014, 404)
     * - SO 소유 지점 불일치: ForbiddenException (SO-013, 403)
     * - DRAFT 아님: InvalidStatusTransitionException (SO-018, 409)
     * - 중복 부품: SalesOrderException (SO-002, 400)
     * - 도착 희망일 범위 초과: SalesOrderException (SO-003, 400)
     * - 창고 미존재: ResourceNotFoundException (SO-015, 404)
     * - 창고 비활성: SalesOrderException (SO-004, 400)
     * - 부품 미존재: ResourceNotFoundException (SO-016, 404)
     */
    @Override
    @Transactional
    public SalesOrder request(RequestSalesOrderCommand command) {
        if (command.role() != UserRole.BRANCH_MANAGER && command.role() != UserRole.BRANCH_STAFF) {
            throw new ForbiddenException(CommonErrorCode.UNAUTHORIZED);
        }

        SalesOrder salesOrder = loadSalesOrderPort.load(command.soCode());

        if (!command.requesterWarehouseCode().equals(salesOrder.getFromWarehouseCode())) {
            throw new ForbiddenException(SalesErrorCode.SO_FORBIDDEN);
        }

        validateNoDuplicateItems(salesOrder.getLines());
        validateDesiredArrivalDate(salesOrder.getDesiredArrivalDate());

        verifyWarehousePort.verify(salesOrder.getFromWarehouseCode());
        verifyWarehousePort.verify(salesOrder.getToWarehouseCode());

        List<String> itemCodes = salesOrder.getLines().stream()
                .map(SalesOrderLine::getItemCode)
                .toList();
        Map<String, ItemInfo> itemMap = loadItemPort.loadAll(itemCodes);

        List<SalesOrderLine> lines = buildLines(salesOrder.getCode(), salesOrder.getLines(), itemMap);

        Instant now = Instant.now();
        salesOrder.submitRequest(
                command.requestedBy(), now,
                salesOrder.getToWarehouseCode(), salesOrder.getDesiredArrivalDate(),
                salesOrder.getRequestMemo(), lines
        );

        SalesOrder saved = saveSalesOrderPort.save(salesOrder);
        appendHistoryPort.append(SalesOrderStatusHistory.of(
                saved.getCode(), SalesOrderStatus.REQUESTED, command.requestedBy(), now));
        return saved;
    }

    private void validateNoDuplicateItems(List<SalesOrderLine> lines) {
        Set<String> seen = new HashSet<>();
        for (SalesOrderLine line : lines) {
            if (!seen.add(line.getItemCode())) {
                throw new SalesOrderException(SalesErrorCode.DUPLICATE_ITEM,
                        "부품 코드 " + line.getItemCode() + "이(가) 중복되었습니다");
            }
        }
    }

    private void validateDesiredArrivalDate(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (!date.isAfter(today)) {
            throw new SalesOrderException(SalesErrorCode.INVALID_DESIRED_ARRIVAL_DATE,
                    "도착 희망일은 오늘 이후여야 합니다");
        }
        if (date.isAfter(today.plusDays(60))) {
            throw new SalesOrderException(SalesErrorCode.INVALID_DESIRED_ARRIVAL_DATE,
                    "도착 희망일은 오늘로부터 60일 이내여야 합니다");
        }
    }

    private List<SalesOrderLine> buildLines(String soCode, List<SalesOrderLine> existingLines,
                                            Map<String, ItemInfo> itemMap) {
        return existingLines.stream()
                .map(line -> {
                    ItemInfo item = itemMap.get(line.getItemCode());
                    return new SalesOrderLine(
                            null, soCode, line.getItemCode(),
                            item.itemName(), item.unit(),
                            line.getQuantity(), line.getPriority()
                    );
                })
                .toList();
    }
}
