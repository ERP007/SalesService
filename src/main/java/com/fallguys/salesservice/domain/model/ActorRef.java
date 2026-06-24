package com.fallguys.salesservice.domain.model;

/**
 * 행위자 참조. code는 항상 보관하고, name·position은 확정 시점에 박제한 스냅샷이다.
 */
public record ActorRef(
        String code,
        String nameSnapshot,
        String positionSnapshot
) {

    public static ActorRef of(String code, String nameSnapshot, String positionSnapshot) {
        return new ActorRef(code, nameSnapshot, positionSnapshot);
    }
}
