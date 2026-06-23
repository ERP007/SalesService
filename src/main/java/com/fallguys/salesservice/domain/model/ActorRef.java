package com.fallguys.salesservice.domain.model;

/**
 * 행위자 참조. code는 항상 보관하고, name·position은 확정 시점에 박제한 스냅샷이다.
 *
 * DRAFT 등 미확정 단계는 code만 들고 snapshot은 null(codeOnly). REQUESTED 이후 확정
 * 행위(요청·승인·배송 등)는 그 시점 JWT의 name·position을 박제한다(of). 박제 후에는
 * 사용자 개명·부서이동과 무관하게 당시 값을 그대로 보존한다.
 */
public record ActorRef(
        String code,
        String nameSnapshot,
        String positionSnapshot
) {
    public static ActorRef codeOnly(String code) {
        return new ActorRef(code, null, null);
    }

    public static ActorRef of(String code, String nameSnapshot, String positionSnapshot) {
        return new ActorRef(code, nameSnapshot, positionSnapshot);
    }
}
