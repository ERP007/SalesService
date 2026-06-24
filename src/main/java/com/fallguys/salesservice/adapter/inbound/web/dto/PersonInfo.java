package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.domain.model.ActorRef;

public record PersonInfo(String code, String name, String position) {
    public static PersonInfo from(ActorRef actor) {
        if (actor == null) return null;
        return new PersonInfo(actor.code(), actor.nameSnapshot(), actor.positionSnapshot());
    }
}
