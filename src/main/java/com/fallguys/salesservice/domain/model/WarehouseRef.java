package com.fallguys.salesservice.domain.model;

/**
 * 창고 참조. code는 항상 보관하고, name은 확정 시점에 박제한 스냅샷이다.
 *
 * DRAFT는 code만 들고 name은 null(codeOnly). REQUESTED 이후 확정 시점에 Inventory에서
 * 조회한 창고명을 박제한다(of). 박제 후에는 창고 개명과 무관하게 당시 값을 보존한다.
 */
public record WarehouseRef(
        String code,
        String nameSnapshot
) {
    public static WarehouseRef codeOnly(String code) {
        return new WarehouseRef(code, null);
    }

    public static WarehouseRef of(String code, String nameSnapshot) {
        return new WarehouseRef(code, nameSnapshot);
    }
}
