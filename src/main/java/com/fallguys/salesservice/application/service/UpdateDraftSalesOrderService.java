package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.command.CreateSalesOrderLineCommand;
import com.fallguys.salesservice.application.port.inbound.command.UpdateDraftSalesOrderCommand;
import com.fallguys.salesservice.application.port.inbound.usecase.UpdateDraftSalesOrderUseCase;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.SaveSalesOrderPort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.WarehouseRef;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorderline.SalesOrderLine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UpdateDraftSalesOrderService implements UpdateDraftSalesOrderUseCase {

    private final LoadSalesOrderPort loadSalesOrderPort;
    private final SaveSalesOrderPort saveSalesOrderPort;

    /**
     * DRAFT 발주를 DRAFT 그대로 수정한다.
     *
     * 흐름:
     * 1) SO 존재 확인 (local DB)
     * 2) SO 소유 지점과 요청자 창고 일치 검증 (local)
     * 3) 중복 부품 코드 검증 (local)
     * 5) 도메인 수정 및 저장 (상태 DRAFT 유지, 외부 호출·스냅샷 없음)
     *
     * 트랜잭션: 쓰기. 조회·수정·저장이 한 트랜잭션으로 묶이며 예외 시 전체 롤백.
     * 임시저장이므로 부품·창고 활성 검증은 제출(submit) 시점으로 미룬다.
     *
     * 예외:
     * - HQ 계열 또는 미허용 역할: ForbiddenException (ER-403, 403)
     * - SO 미존재: ResourceNotFoundException (SO-014, 404)
     * - SO 소유 지점 불일치: ForbiddenException (SO-013, 403)
     * - DRAFT 아님: InvalidStatusTransitionException (SO-018, 409)
     * - 중복 부품: SalesOrderException (SO-002, 400)
     */
    @Override
    @Transactional
    public SalesOrder updateDraft(UpdateDraftSalesOrderCommand command) {
        if (!command.role().isBranchUser()) {
            throw new ForbiddenException(CommonErrorCode.UNAUTHORIZED);
        }

        SalesOrder salesOrder = loadSalesOrderPort.load(command.soCode());

        if (!command.requesterWarehouseCode().equals(salesOrder.getFrom().code())) {
            throw new ForbiddenException(SalesErrorCode.SO_FORBIDDEN);
        }

        validateNoDuplicateItems(command.lines());

        List<SalesOrderLine> lines = buildDraftLines(salesOrder.getCode(), command.lines());
        // DRAFT 유지 — 창고명은 박제하지 않고 code만 보관(codeOnly).
        salesOrder.updateDraft(
                WarehouseRef.codeOnly(command.toWarehouseCode()),
                command.requestMemo(), lines
        );

        return saveSalesOrderPort.save(salesOrder);
    }

    private void validateNoDuplicateItems(List<CreateSalesOrderLineCommand> lines) {
        Set<String> seen = new HashSet<>();
        for (CreateSalesOrderLineCommand line : lines) {
            if (!seen.add(line.itemCode())) {
                throw new SalesOrderException(SalesErrorCode.DUPLICATE_ITEM,
                        "부품 코드 " + line.itemCode() + "이(가) 중복되었습니다");
            }
        }
    }

    private List<SalesOrderLine> buildDraftLines(String soCode, List<CreateSalesOrderLineCommand> lineCommands) {
        return lineCommands.stream()
                .map(cmd -> new SalesOrderLine(
                        null, soCode, cmd.itemCode(),
                        null, null,
                        cmd.quantity(), cmd.priority()
                ))
                .toList();
    }
}
